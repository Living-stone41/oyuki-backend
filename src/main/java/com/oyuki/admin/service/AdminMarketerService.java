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
    public AdminMarketerResponse create(CreateMarketerRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phoneNumber().replace(" ", "").replace("-", "").trim();
        if (userRepository.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("This email address is already registered");
        if (userRepository.existsByPhoneNumber(phone)) throw new IllegalArgumentException("This phone number is already registered");

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phoneNumber(phone)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.MARKETER)
                .status(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        user = userRepository.save(user);
        referralService.ensureReferralCode(user);

        String otp = otpGenerator.generateSixDigitOtp();
        tokenRepository.save(VerificationToken.builder()
                .user(user).tokenHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(10)).used(false).attempts(0).build());
        otpDeliveryService.sendRegistrationOtp(user, otp);
        return AdminMarketerResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<AdminMarketerResponse> list() {
        return userRepository.findAllByRole(Role.MARKETER).stream().map(AdminMarketerResponse::from).toList();
    }
}
