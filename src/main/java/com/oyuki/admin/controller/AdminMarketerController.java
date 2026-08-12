package com.oyuki.admin.controller;

<<<<<<< HEAD
import com.oyuki.referral.service.ReferralService;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
=======
import com.oyuki.admin.dto.AdminMarketerResponse;
import com.oyuki.admin.dto.CreateMarketerRequest;
import com.oyuki.admin.service.AdminMarketerService;
import com.oyuki.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
>>>>>>> 1f72347 (Update Oyuki backend)

@RestController
@RequestMapping("/api/admin/marketers")
@RequiredArgsConstructor
public class AdminMarketerController {
<<<<<<< HEAD

    private final UserRepository userRepository;
    private final ReferralService referralService;

    /**
     * Promotes an existing account to MARKETER. Marketers cannot self-register
     * publicly; the admin must create a normal account first or promote an
     * existing account through this endpoint.
     */
    @PatchMapping("/{userId}/promote")
    public Map<String, Object> promote(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(Role.MARKETER);
        referralService.ensureReferralCode(user);
        userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("fullName", user.getFullName());
        result.put("role", user.getRole());
        result.put("referralCode", user.getReferralCode());
        result.put("message", "Marketer account created successfully");
        return result;
    }

    @PatchMapping("/{userId}/remove")
    public Map<String, Object> remove(@PathVariable Long userId,
                                      @RequestParam(defaultValue = "CUSTOMER") Role fallbackRole) {
        if (fallbackRole == Role.ADMIN || fallbackRole == Role.MARKETER
                || fallbackRole == Role.ACCOUNT_OFFICER || fallbackRole == Role.LOGISTICS_ADMIN) {
            throw new IllegalArgumentException("Choose CUSTOMER, SELLER, KITCHEN or RIDER as the fallback role");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(fallbackRole);
        userRepository.save(user);
        return Map.of("userId", user.getId(), "role", user.getRole(), "message", "Marketer access removed");
=======
    private final AdminMarketerService service;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminMarketerResponse>> create(@Valid @RequestBody CreateMarketerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Marketer created. An OTP has been sent for account activation.", service.create(request)));
    }

    @GetMapping
    public ApiResponse<List<AdminMarketerResponse>> list() {
        return ApiResponse.success("Marketers retrieved successfully", service.list());
>>>>>>> 1f72347 (Update Oyuki backend)
    }
}
