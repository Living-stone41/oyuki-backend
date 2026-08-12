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

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketerActivationService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, Object> activate(ActivateMarketerRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }
        String contact = request.contact().trim();
        User user = userRepository.findByEmailIgnoreCaseOrPhoneNumber(contact.toLowerCase(Locale.ROOT), contact)
                .orElseThrow(() -> new IllegalArgumentException("Marketer account was not found"));
        if (user.getRole() != Role.MARKETER) throw new IllegalStateException("This is not a marketer account");
        if (user.getStatus() != AccountStatus.PENDING_VERIFICATION) throw new IllegalStateException("This account is not awaiting verification");

        VerificationToken token = tokenRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No active verification code was found"));
        if (token.isExpired()) throw new IllegalArgumentException("Verification code has expired");
        if (token.hasExceededAttempts()) throw new IllegalArgumentException("Too many incorrect attempts. Request another code.");
        if (!passwordEncoder.matches(request.token(), token.getTokenHash())) {
            token.setAttempts(token.getAttempts() + 1); tokenRepository.save(token);
            throw new IllegalArgumentException("Incorrect verification code");
        }
        token.setUsed(true); tokenRepository.save(token);
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(contact)) user.setEmailVerified(true);
        else if (user.getPhoneNumber() != null && user.getPhoneNumber().equals(contact)) user.setPhoneVerified(true);
        else throw new IllegalArgumentException("The supplied contact does not belong to this account");
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(AccountStatus.ACTIVE);
        user.setStatusReason(null);
        userRepository.save(user);
        return Map.of("userId", user.getId(), "role", user.getRole(), "status", user.getStatus(), "referralCode", user.getReferralCode());
    }
}
