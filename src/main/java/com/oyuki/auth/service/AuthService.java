package com.oyuki.auth.service;

import com.oyuki.auth.dto.RegisterRequest;
import com.oyuki.auth.dto.VerifyRegistrationRequest;
import com.oyuki.auth.entity.VerificationToken;
import com.oyuki.auth.enums.OtpChannel;
import com.oyuki.auth.repository.VerificationTokenRepository;
import com.oyuki.common.util.OtpGenerator;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    /*
     * These are the roles users are allowed
     * to select during public registration.
     *
     * ADMIN and LOGISTICS_ADMIN must not be
     * available through public registration.
     */
    private static final Set<Role> PUBLIC_REGISTRATION_ROLES =
            EnumSet.of(
                    Role.CUSTOMER,
                    Role.SELLER,
                    Role.KITCHEN,
                    Role.RIDER
            );

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final OtpDeliveryService otpDeliveryService;

    /*
     * REGISTER A NEW USER
     */
    @Transactional
    public Map<String, Object> register(
            RegisterRequest request
    ) {
        String email =
                normalizeEmail(request.email());

        String phoneNumber =
                normalizePhone(request.phoneNumber());

        validateContactDetails(
                email,
                phoneNumber
        );

        validatePasswords(
                request.password(),
                request.confirmPassword()
        );

        validatePublicRole(request.role());

        validateDuplicateContact(
                email,
                phoneNumber
        );

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phoneNumber(phoneNumber)
                .passwordHash(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .role(request.role())
                .status(
                        AccountStatus.PENDING_VERIFICATION
                )
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        User savedUser =
                userRepository.save(user);

        String otp =
                otpGenerator.generateSixDigitOtp();

        VerificationToken token =
                VerificationToken.builder()
                        .user(savedUser)
                        .tokenHash(
                                passwordEncoder.encode(otp)
                        )
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(10)
                        )
                        .used(false)
                        .attempts(0)
                        .build();

        verificationTokenRepository.save(token);

        String contact =
                otpDeliveryService.sendRegistrationOtp(
                        savedUser,
                        otp,
                        request.otpChannel()
                );

        return Map.of(
                "userId",
                savedUser.getId(),

                "contact",
                maskContact(contact),

                "role",
                savedUser.getRole(),

                "status",
                savedUser.getStatus()
        );
    }

    /* RESEND REGISTRATION OTP */
    @Transactional
    public Map<String, Object> resendRegistrationOtp(
            com.oyuki.auth.dto.ResendRegistrationOtpRequest request
    ) {
        String contact = request.contact().trim();
        User user = userRepository
                .findByEmailIgnoreCaseOrPhoneNumber(
                        contact.toLowerCase(Locale.ROOT), contact)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pending registration was found for this email or phone number"));

        if (user.getStatus() != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("This account has already been verified");
        }

        verificationTokenRepository
                .findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .ifPresent(existing -> {
                    if (existing.getCreatedAt() != null
                            && existing.getCreatedAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
                        long remaining = java.time.Duration.between(
                                LocalDateTime.now(), existing.getCreatedAt().plusSeconds(60)).getSeconds();
                        throw new IllegalStateException(
                                "Please wait " + Math.max(remaining, 1)
                                        + " seconds before requesting another code");
                    }
                    existing.setUsed(true);
                    verificationTokenRepository.save(existing);
                });

        String otp = otpGenerator.generateSixDigitOtp();
        VerificationToken token = VerificationToken.builder()
                .user(user)
                .tokenHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
        verificationTokenRepository.save(token);
        otpDeliveryService.sendRegistrationOtpToContact(user, otp, contact);

        return Map.of(
                "contact", maskContact(contact),
                "expiresInMinutes", 10,
                "resendAvailableInSeconds", 60);
    }

    /*
     * VERIFY REGISTRATION OTP
     */
    @Transactional
    public Map<String, Object> verifyRegistration(
            VerifyRegistrationRequest request
    ) {
        String contact =
                request.contact().trim();

        User user = userRepository
                .findByEmailIgnoreCaseOrPhoneNumber(
                        contact.toLowerCase(
                                Locale.ROOT
                        ),
                        contact
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email address or phone number"
                        )
                );

        if (
                user.getStatus()
                        != AccountStatus.PENDING_VERIFICATION
        ) {
            throw new IllegalStateException(
                    "This account has already been verified"
            );
        }

        VerificationToken verificationToken =
                verificationTokenRepository
                        .findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No active verification code was found"
                                )
                        );

        if (verificationToken.isExpired()) {
            throw new IllegalArgumentException(
                    "Verification code has expired"
            );
        }

        if (
                verificationToken
                        .hasExceededAttempts()
        ) {
            throw new IllegalArgumentException(
                    "Too many incorrect attempts. Request another code."
            );
        }

        boolean validToken =
                passwordEncoder.matches(
                        request.token(),
                        verificationToken.getTokenHash()
                );

        if (!validToken) {
            verificationToken.setAttempts(
                    verificationToken.getAttempts() + 1
            );

            verificationTokenRepository.save(
                    verificationToken
            );

            throw new IllegalArgumentException(
                    "Incorrect verification code"
            );
        }

        verificationToken.setUsed(true);

        verificationTokenRepository.save(
                verificationToken
        );

        verifyContact(
                user,
                contact
        );

        updateAccountStatusAfterVerification(
                user
        );

        User verifiedUser =
                userRepository.save(user);

        return Map.of(
                "userId",
                verifiedUser.getId(),

                "role",
                verifiedUser.getRole(),

                "status",
                verifiedUser.getStatus(),

                "emailVerified",
                verifiedUser.isEmailVerified(),

                "phoneVerified",
                verifiedUser.isPhoneVerified()
        );
    }

    /*
     * VALIDATE EMAIL OR PHONE
     */
    private void validateContactDetails(
            String email,
            String phoneNumber
    ) {
        if (
                email == null &&
                phoneNumber == null
        ) {
            throw new IllegalArgumentException(
                    "Email address or phone number is required"
            );
        }
    }

    /*
     * VALIDATE PASSWORDS
     */
    private void validatePasswords(
            String password,
            String confirmPassword
    ) {
        if (
                password == null ||
                confirmPassword == null
        ) {
            throw new IllegalArgumentException(
                    "Password and confirm password are required"
            );
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException(
                    "Password and confirm password do not match"
            );
        }
    }

    /*
     * VALIDATE PUBLIC REGISTRATION ROLE
     */
    private void validatePublicRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException(
                    "Account role is required"
            );
        }

        if (
                !PUBLIC_REGISTRATION_ROLES
                        .contains(role)
        ) {
            throw new IllegalArgumentException(
                    "You cannot register publicly with this role"
            );
        }
    }

    /*
     * CHECK DUPLICATE EMAIL OR PHONE NUMBER
     */
    private void validateDuplicateContact(
            String email,
            String phoneNumber
    ) {
        if (
                email != null &&
                userRepository
                        .existsByEmailIgnoreCase(email)
        ) {
            throw new IllegalArgumentException(
                    "This email address is already registered"
            );
        }

        if (
                phoneNumber != null &&
                userRepository
                        .existsByPhoneNumber(phoneNumber)
        ) {
            throw new IllegalArgumentException(
                    "This phone number is already registered"
            );
        }
    }

    /*
     * MARK EMAIL OR PHONE AS VERIFIED
     */
    private void verifyContact(
            User user,
            String suppliedContact
    ) {
        if (
                user.getEmail() != null &&
                user.getEmail()
                        .equalsIgnoreCase(
                                suppliedContact
                        )
        ) {
            user.setEmailVerified(true);
            return;
        }

        if (
                user.getPhoneNumber() != null &&
                user.getPhoneNumber()
                        .equals(suppliedContact)
        ) {
            user.setPhoneVerified(true);
            return;
        }

        throw new IllegalArgumentException(
                "The supplied contact does not belong to this account"
        );
    }

    /*
     * SET ACCOUNT STATUS AFTER OTP VERIFICATION
     */
    private void updateAccountStatusAfterVerification(
            User user
    ) {
        /*
         * Customers become active immediately.
         */
        if (user.getRole() == Role.CUSTOMER) {
            user.setStatus(
                    AccountStatus.ACTIVE
            );

            return;
        }

        /*
         * Sellers, kitchens and riders must
         * wait for administrator approval.
         */
        if (
                user.getRole() == Role.SELLER ||
                user.getRole() == Role.KITCHEN ||
                user.getRole() == Role.RIDER
        ) {
            user.setStatus(
                    AccountStatus.PENDING_APPROVAL
            );

            return;
        }

        throw new IllegalStateException(
                "Unsupported registration role"
        );
    }

    /*
     * NORMALIZE EMAIL
     */
    private String normalizeEmail(
            String email
    ) {
        if (
                email == null ||
                email.isBlank()
        ) {
            return null;
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /*
     * NORMALIZE PHONE NUMBER
     */
    private String normalizePhone(
            String phoneNumber
    ) {
        if (
                phoneNumber == null ||
                phoneNumber.isBlank()
        ) {
            return null;
        }

        return phoneNumber
                .replace(" ", "")
                .replace("-", "")
                .trim();
    }

    /*
     * HIDE PART OF EMAIL OR PHONE
     */
    private String maskContact(
            String contact
    ) {
        if (contact.contains("@")) {
            String[] parts =
                    contact.split("@", 2);

            String username = parts[0];

            String visibleUsername =
                    username.length() <= 2
                            ? username.substring(0, 1)
                            : username.substring(0, 2);

                            
            return visibleUsername
                    + "***@"
                    + parts[1];
        }

        if (contact.length() <= 4) {
            return "****";
        }

        return "*".repeat(
                contact.length() - 4
        ) + contact.substring(
                contact.length() - 4
        );
    }
}