package com.oyuki.auth.service;

import com.oyuki.auth.dto.ForgotPasswordRequest;
import com.oyuki.auth.dto.ResetPasswordRequest;
import com.oyuki.auth.dto.VerifyPasswordResetRequest;
import com.oyuki.auth.entity.PasswordResetToken;
import com.oyuki.auth.repository.PasswordResetTokenRepository;
import com.oyuki.common.util.OtpGenerator;
import com.oyuki.user.entity.User;
import com.oyuki.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final OtpDeliveryService otpDeliveryService;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            OtpGenerator otpGenerator,
            OtpDeliveryService otpDeliveryService
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpGenerator = otpGenerator;
        this.otpDeliveryService = otpDeliveryService;
    }

    @Transactional
    public Map<String, Object> requestPasswordReset(
            ForgotPasswordRequest request
    ) {
        String contact = normalizeContact(request.contact());

        Optional<User> optionalUser = findUserOptional(contact);

        /*
         * Do not reveal whether the account exists.
         */
        if (optionalUser.isEmpty()) {
            return Map.of("requested", true);
        }

        User user = optionalUser.get();

        invalidatePreviousTokens(user.getId());

        String otp = otpGenerator.generateSixDigitOtp();

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .user(user)
                        .tokenHash(passwordEncoder.encode(otp))
                        .expiresAt(LocalDateTime.now().plusMinutes(10))
                        .verified(false)
                        .used(false)
                        .attempts(0)
                        .build();

        tokenRepository.save(resetToken);

        otpDeliveryService.sendPasswordResetOtp(
                user,
                contact,
                otp
        );

        return Map.of("requested", true);
    }

    @Transactional
    public Map<String, Object> verifyResetToken(
            VerifyPasswordResetRequest request
    ) {
        String contact = normalizeContact(request.contact());

        User user = findUser(contact);
        PasswordResetToken token = findActiveToken(user);

        validateTokenState(token);

        if (!passwordEncoder.matches(
                request.token(),
                token.getTokenHash()
        )) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Incorrect password reset code"
            );
        }

        token.setVerified(true);
        tokenRepository.save(token);

        return Map.of(
                "verified", true,
                "message", "Reset code verified"
        );
    }

    @Transactional
    public Map<String, Object> resetPassword(
            ResetPasswordRequest request
    ) {
        validatePasswords(
                request.newPassword(),
                request.confirmPassword()
        );

        String contact = normalizeContact(request.contact());

        User user = findUser(contact);
        PasswordResetToken token = findActiveToken(user);

        validateTokenState(token);

        if (!token.isVerified()) {
            throw new IllegalStateException(
                    "Verify the password reset code first"
            );
        }

        /*
         * Check the OTP again before changing the password.
         */
        if (!passwordEncoder.matches(
                request.token(),
                token.getTokenHash()
        )) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);

            throw new IllegalArgumentException(
                    "Incorrect password reset code"
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "New password must be different from the old password"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        /*
         * Invalidate JWTs created before this password reset.
         */
        user.setTokenVersion(
                user.getTokenVersion() + 1
        );

        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        return Map.of(
                "passwordReset", true,
                "message", "Password updated successfully"
        );
    }

    private Optional<User> findUserOptional(String contact) {

        String email = contact.toLowerCase(Locale.ROOT);

        String phone = contact
                .replace(" ", "")
                .replace("-", "");

        return userRepository
                .findByEmailIgnoreCaseOrPhoneNumber(
                        email,
                        phone
                );
    }

    private User findUser(String contact) {
        return findUserOptional(contact)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid or expired password reset request"
                        )
                );
    }

    private PasswordResetToken findActiveToken(User user) {
        return tokenRepository
                .findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(
                        user.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid or expired password reset request"
                        )
                );
    }

    private void validateTokenState(
            PasswordResetToken token
    ) {
        if (token.isUsed()) {
            throw new IllegalArgumentException(
                    "This password reset code has already been used"
            );
        }

        if (token.isExpired()) {
            throw new IllegalArgumentException(
                    "Password reset code has expired"
            );
        }

        if (token.hasExceededAttempts()) {
            throw new IllegalArgumentException(
                    "Too many incorrect attempts. Request another code."
            );
        }
    }

    private void validatePasswords(
            String password,
            String confirmPassword
    ) {
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException(
                    "New password and confirm password do not match"
            );
        }
    }

    private void invalidatePreviousTokens(Long userId) {

        List<PasswordResetToken> activeTokens =
                tokenRepository.findAllByUserIdAndUsedFalse(userId);

        activeTokens.forEach(token ->
                token.setUsed(true)
        );

        tokenRepository.saveAll(activeTokens);
    }

    private String normalizeContact(String contact) {
        return contact.trim();
    }
}