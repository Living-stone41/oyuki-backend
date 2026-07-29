package com.oyuki.referral.controller;

import com.oyuki.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {
    private final ReferralService referralService;

    @GetMapping("/me")
    public Map<String, Object> mine(Authentication authentication) {
        return referralService.getMyReferralSummary((Long) authentication.getPrincipal());
    }
}
