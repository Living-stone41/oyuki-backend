package com.oyuki.referral.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReferralCodeBackfill {
    private final ReferralService referralService;

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        referralService.backfillMissingReferralCodes();
    }
}
