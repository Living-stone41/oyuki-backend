package com.oyuki.user.controller;

import com.oyuki.common.response.ApiResponse;
import com.oyuki.user.entity.User;
import com.oyuki.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class CurrentUserController {

    private final UserRepository userRepository;

    public CurrentUserController(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User account was not found"
                        )
                );

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put("id", user.getId());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("phoneNumber", user.getPhoneNumber());
        data.put("role", user.getRole());
        data.put("status", user.getStatus());
        data.put("emailVerified", user.isEmailVerified());
        data.put("phoneVerified", user.isPhoneVerified());

        return ApiResponse.success(
                "Current user retrieved successfully",
                data
        );
    }
}