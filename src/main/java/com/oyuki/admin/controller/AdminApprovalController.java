package com.oyuki.admin.controller;

import com.oyuki.admin.dto.AdminApplicationResponse;
import com.oyuki.admin.dto.RejectApplicationRequest;
import com.oyuki.admin.service.AdminApprovalService;
import com.oyuki.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/applications")
public class AdminApprovalController {

    private final AdminApprovalService adminApprovalService;

    public AdminApprovalController(
            AdminApprovalService adminApprovalService
    ) {
        this.adminApprovalService =
                adminApprovalService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<AdminApplicationResponse>>
    getPendingApplications() {

        return ApiResponse.success(
                "Pending applications retrieved successfully",
                adminApprovalService.getPendingApplications()
        );
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminApplicationResponse>
    getApplication(
            @PathVariable Long userId
    ) {
        return ApiResponse.success(
                "Application retrieved successfully",
                adminApprovalService.getApplication(userId)
        );
    }

    @PatchMapping("/{userId}/approve")
    public ApiResponse<AdminApplicationResponse>
    approveApplication(
            @PathVariable Long userId
    ) {
        return ApiResponse.success(
                "Application approved successfully",
                adminApprovalService.approveApplication(userId)
        );
    }

    @PatchMapping("/{userId}/reject")
    public ApiResponse<AdminApplicationResponse>
    rejectApplication(
            @PathVariable Long userId,
            @Valid @RequestBody RejectApplicationRequest request
    ) {
        return ApiResponse.success(
                "Application rejected successfully",
                adminApprovalService.rejectApplication(
                        userId,
                        request
                )
        );
    }
}