package com.oyuki.auth.service;

import com.oyuki.auth.dto.LoginRequest;
import com.oyuki.auth.dto.LoginResponse;
import com.oyuki.security.JwtService;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        String identifier =
                request.identifier().trim();

        String emailIdentifier =
                identifier.toLowerCase(Locale.ROOT);

        String phoneIdentifier =
                identifier
                        .replace(" ", "")
                        .replace("-", "");

        User user = userRepository
                .findByEmailIgnoreCaseOrPhoneNumber(
                        emailIdentifier,
                        phoneIdentifier
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email, phone number, or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Invalid email, phone number, or password"
            );
        }

        validateAccountStatus(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getJwtExpiration(),
                user.getId(),
                user.getFullName(),
                user.getRole(),
                user.getStatus()
        );
    }
private void validateAccountStatus(User user) {

    AccountStatus status = user.getStatus();

    switch (status) {

        case ACTIVE, PENDING_APPROVAL -> {
            // ACTIVE users can use the platform.
            // PENDING_APPROVAL sellers and kitchens can complete their profile.
        }

        case PENDING_VERIFICATION ->
                throw new IllegalStateException(
                        "Please verify your email address or phone number"
                );

        case REJECTED -> {
            String reason = user.getStatusReason() == null
                    ? "Your registration was rejected"
                    : user.getStatusReason();

            throw new IllegalStateException(reason);
        }

        case SUSPENDED -> {
            String reason = user.getStatusReason() == null
                    ? "Your account has been suspended"
                    : user.getStatusReason();

            throw new IllegalStateException(reason);
        }

        case DISABLED ->
                throw new IllegalStateException(
                        "Your account has been disabled"
                );
    }
}
}