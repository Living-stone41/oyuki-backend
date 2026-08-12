package com.oyuki.admin.service;

import com.oyuki.admin.dto.AdminMarketerResponse;
import com.oyuki.admin.dto.CreateMarketerRequest;
import com.oyuki.auth.entity.VerificationToken;
import com.oyuki.auth.repository.VerificationTokenRepository;
import com.oyuki.auth.service.OtpDeliveryService;
import com.oyuki.common.util.OtpGenerator;
import com.oyuki.referral.service.ReferralService;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminMarketerService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final OtpDeliveryService otpDeliveryService;
    private final ReferralService referralService;

    @Transactional
    public AdminMarketerResponse create(
            CreateMarketerRequest request
    ) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        String phone = request.phoneNumber()
                .replace(" ", "")
                .replace("-", "")
                .trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "This email address is already registered"
            );
        }

        if (userRepository.existsByPhoneNumber(phone)) {
            throw new IllegalArgumentException(
                    "This phone number is already registered"
            );
        }

        /*
         * Marketers are created only by admin.
         * They start in PENDING_VERIFICATION.
         *
         * A temporary random password is stored because
         * password_hash cannot be null.
         *
         * The marketer will set their real password after
         * completing OTP verification.
         */
        User marketer = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phoneNumber(phone)
                .passwordHash(
                        passwordEncoder.encode(
                                UUID.randomUUID().toString()
                        )
                )
                .role(Role.MARKETER)
                .status(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        marketer = userRepository.save(marketer);

        /*
         * Automatically generate and save the marketer's
         * unique referral code.
         */
        referralService.ensureReferralCode(marketer);

        /*
         * Generate registration OTP.
         */
        String otp =
                otpGenerator.generateSixDigitOtp();

        /*
         * Store only the BCrypt hash of the OTP.
         */
        VerificationToken token =
                VerificationToken.builder()
                        .user(marketer)
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

        tokenRepository.save(token);

        /*
         * Send OTP through the existing Oyuki OTP
         * delivery system.
         */
        otpDeliveryService.sendRegistrationOtp(
                marketer,
                otp
        );

        return AdminMarketerResponse.from(marketer);
    }

    @Transactional(readOnly = true)
    public List<AdminMarketerResponse> list() {

        return userRepository
                .findAllByRole(Role.MARKETER)
                .stream()
                .map(AdminMarketerResponse::from)
                .toList();
    }
}