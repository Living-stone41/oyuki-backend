package com.oyuki.auth.service;

import com.oyuki.auth.dto.ActivateMarketerRequest;
import com.oyuki.auth.entity.VerificationToken;
import com.oyuki.auth.repository.VerificationTokenRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketerActivationService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TwilioVerifyService twilioVerifyService;

    @Transactional
    public Map<String, Object> activate(ActivateMarketerRequest request) {
        validatePassword(request);

        String email = clean(request.email());
        String phone = clean(request.phoneNumber());
        if (email == null && phone == null) {
            throw new IllegalArgumentException("Enter the email or phone number used for this account");
        }
        if (blank(request.emailOtp()) && blank(request.phoneOtp())) {
            throw new IllegalArgumentException("Enter at least one verification code");
        }

        User user = findUser(email, phone);
        if (user.getRole() != Role.MARKETER) throw new IllegalStateException("This is not a marketer account");
        if (user.getStatus() != AccountStatus.PENDING_VERIFICATION) throw new IllegalStateException("This account is not awaiting verification");

        validateContactsBelongToUser(user, email, phone);

        boolean emailVerified = false;
        boolean phoneVerified = false;

        if (!blank(request.emailOtp()) && user.getEmail() != null) {
            VerificationToken token = tokenRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId()).orElse(null);
            if (token != null && !token.isExpired() && !token.hasExceededAttempts()) {
                emailVerified = passwordEncoder.matches(request.emailOtp().trim(), token.getTokenHash());
                if (emailVerified) {
                    token.setUsed(true);
                    tokenRepository.save(token);
                    user.setEmailVerified(true);
                } else {
                    token.setAttempts(token.getAttempts() + 1);
                    tokenRepository.save(token);
                }
            }
        }

        if (!blank(request.phoneOtp()) && user.getPhoneNumber() != null) {
            phoneVerified = twilioVerifyService.verifyOtp(user.getPhoneNumber(), request.phoneOtp().trim());
            if (phoneVerified) user.setPhoneVerified(true);
        }

        if (!emailVerified && !phoneVerified) {
            throw new IllegalArgumentException("Incorrect or expired verification code. Enter a valid email OTP or phone OTP.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(AccountStatus.ACTIVE);
        user.setStatusReason(null);
        userRepository.save(user);

        return Map.of("userId", user.getId(), "role", user.getRole(), "status", user.getStatus(), "referralCode", user.getReferralCode());
    }

    private User findUser(String email, String phone) {
        if (email != null) {
            return userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new IllegalArgumentException("Marketer account was not found"));
        }
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new IllegalArgumentException("Marketer account was not found"));
    }

    private void validateContactsBelongToUser(User user, String email, String phone) {
        if (email != null && (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)))
            throw new IllegalArgumentException("The supplied email does not belong to this account");
        if (phone != null && (user.getPhoneNumber() == null || !user.getPhoneNumber().equals(phone)))
            throw new IllegalArgumentException("The supplied phone number does not belong to this account");
    }

    private void validatePassword(ActivateMarketerRequest request) {
        if (!request.password().equals(request.confirmPassword()))
            throw new IllegalArgumentException("Password and confirm password do not match");
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
