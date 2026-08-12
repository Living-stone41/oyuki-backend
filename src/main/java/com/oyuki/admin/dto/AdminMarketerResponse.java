package com.oyuki.admin.dto;

import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import java.time.LocalDateTime;

public record AdminMarketerResponse(
        Long userId,
        String fullName,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        String referralCode,
        LocalDateTime createdAt
) {
    public static AdminMarketerResponse from(User user) {
        return new AdminMarketerResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getStatus(), user.getReferralCode(), user.getCreatedAt());
    }
}
