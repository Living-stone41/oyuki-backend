package com.oyuki.auth.dto;

import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        Long userId,
        String fullName,
        Role role,
        AccountStatus status
) {
}