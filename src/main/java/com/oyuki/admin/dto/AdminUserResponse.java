package com.oyuki.admin.dto;

import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;

import java.time.LocalDateTime;

public record AdminUserResponse(

        Long id,
        String fullName,
        String email,
        String phoneNumber,
        Role role,
        AccountStatus status,
        String statusReason,
        boolean emailVerified,
        boolean phoneVerified,
        String profileImageUrl,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static AdminUserResponse from(
            User user
    ) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus(),
                user.getStatusReason(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getProfileImageUrl(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
