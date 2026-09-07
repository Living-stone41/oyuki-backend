package com.oyuki.admin.service;

import com.oyuki.admin.dto.AdminMarketAgentResponse;
import com.oyuki.admin.dto.CreateMarketAgentRequest;
import com.oyuki.auth.entity.VerificationToken;
import com.oyuki.auth.repository.VerificationTokenRepository;
import com.oyuki.auth.service.OtpDeliveryService;
import com.oyuki.common.util.OtpGenerator;
import com.oyuki.marketsquare.entity.LocalGovernment;
import com.oyuki.marketsquare.entity.Market;
import com.oyuki.marketsquare.entity.MarketAgentProfile;
import com.oyuki.marketsquare.repository.LocalGovernmentRepository;
import com.oyuki.marketsquare.repository.MarketAgentProfileRepository;
import com.oyuki.marketsquare.repository.MarketRepository;
import com.oyuki.marketsquare.repository.StateRepository;
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
public class AdminMarketAgentService {
    private final UserRepository userRepository;
    private final MarketAgentProfileRepository profileRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final OtpDeliveryService otpDeliveryService;
    private final StateRepository stateRepository;
    private final LocalGovernmentRepository lgaRepository;
    private final MarketRepository marketRepository;

    @Transactional
    public AdminMarketAgentResponse create(CreateMarketAgentRequest request) {
        String email = cleanEmail(request.email());
        String phone = cleanPhone(request.phoneNumber());

        if (email == null && phone == null) {
            throw new IllegalArgumentException("Provide at least an email address or phone number");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("This email address is already registered");
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new IllegalArgumentException("This phone number is already registered");
        }

        var state = stateRepository.findById(request.stateId())
                .orElseThrow(() -> new IllegalArgumentException("Selected state was not found"));
        LocalGovernment lga = lgaRepository.findById(request.lgaId())
                .orElseThrow(() -> new IllegalArgumentException("Selected LGA was not found"));
        Market market = marketRepository.findById(request.marketId())
                .orElseThrow(() -> new IllegalArgumentException("Selected market was not found"));

        if (lga.getState() == null || !lga.getState().getId().equals(state.getId())) {
            throw new IllegalArgumentException("The selected LGA does not belong to the selected state");
        }
        if (market.getLga() == null || !market.getLga().getId().equals(lga.getId())) {
            throw new IllegalArgumentException("The selected market does not belong to the selected LGA");
        }
        if (!lga.isActive() || !market.isActive()) {
            throw new IllegalArgumentException("The selected LGA or market is inactive");
        }

        User agent = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phoneNumber(phone)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.MARKET_AGENT)
                .status(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        agent = userRepository.save(agent);

        MarketAgentProfile profile = MarketAgentProfile.builder()
                .user(agent)
                .assignedLga(lga)
                .assignedMarket(market)
                .emergencyContactName(blankToNull(request.emergencyContactName()))
                .emergencyContactPhone(blankToNull(request.emergencyContactPhone()))
                .build();
        profileRepository.save(profile);

        String otp = otpGenerator.generateSixDigitOtp();
        tokenRepository.save(VerificationToken.builder()
                .user(agent)
                .tokenHash(passwordEncoder.encode(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build());

        otpDeliveryService.sendAdminActivationOtps(agent, otp);
        return AdminMarketAgentResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public List<AdminMarketAgentResponse> list() {
        return profileRepository.findAll().stream()
                .filter(p -> p.getUser() != null && p.getUser().getRole() == Role.MARKET_AGENT)
                .map(AdminMarketAgentResponse::from)
                .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private String cleanEmail(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanPhone(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replace(" ", "").replace("-", "").trim();
    }

}
