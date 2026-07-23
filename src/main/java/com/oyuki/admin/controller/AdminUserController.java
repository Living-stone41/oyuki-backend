package com.oyuki.admin.controller;

import com.oyuki.admin.dto.AdminUserResponse;
import com.oyuki.admin.dto.AdminUserStatisticsResponse;
import com.oyuki.admin.dto.UpdateUserStatusRequest;
import com.oyuki.admin.service.AdminUserService;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(
            AdminUserService adminUserService
    ) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>>
    getUsers(
            Authentication authentication,
            @RequestParam(required = false)
            Role role,
            @RequestParam(required = false)
            AccountStatus status
    ) {
        return ResponseEntity.ok(
                adminUserService.getUsers(
                        getUserId(authentication),
                        role,
                        status
                )
        );
    }

    @GetMapping("/statistics")
    public ResponseEntity<AdminUserStatisticsResponse>
    getStatistics(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                adminUserService.getStatistics(
                        getUserId(authentication)
                )
        );
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserResponse>
    updateStatus(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody
            UpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(
                adminUserService.updateStatus(
                        getUserId(authentication),
                        userId,
                        request
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}
