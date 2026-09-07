package com.oyuki.admin.dto;

import com.oyuki.marketsquare.entity.MarketAgentProfile;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.entity.User;
import java.time.LocalDateTime;

public record AdminMarketAgentResponse(
        Long userId,
        Long profileId,
        String fullName,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        String lgaName,
        String marketName,
        String stateName,
        String emergencyContactName,
        String emergencyContactPhone,
        LocalDateTime createdAt
) {
    public static AdminMarketAgentResponse from(MarketAgentProfile profile) {
        User user = profile.getUser();
        return new AdminMarketAgentResponse(
                user.getId(), profile.getId(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getStatus(),
                profile.getAssignedLga() == null ? null : profile.getAssignedLga().getName(),
                profile.getAssignedMarket() == null ? null : profile.getAssignedMarket().getName(),
                profile.getAssignedLga() == null || profile.getAssignedLga().getState() == null
                        ? null : profile.getAssignedLga().getState().getName(),
                profile.getEmergencyContactName(), profile.getEmergencyContactPhone(),
                user.getCreatedAt()
        );
    }
}
