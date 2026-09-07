package com.oyuki.admin.controller;

import com.oyuki.admin.dto.AdminMarketAgentResponse;
import com.oyuki.admin.dto.CreateMarketAgentRequest;
import com.oyuki.admin.service.AdminMarketAgentService;
import com.oyuki.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/market-agents")
@RequiredArgsConstructor
public class AdminMarketAgentController {
    private final AdminMarketAgentService service;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminMarketAgentResponse>> create(
            @Valid @RequestBody CreateMarketAgentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Market Agent created. An OTP has been sent for account activation.",
                        service.create(request)
                )
        );
    }

    @GetMapping
    public ApiResponse<List<AdminMarketAgentResponse>> list() {
        return ApiResponse.success("Market Agents retrieved successfully", service.list());
    }
}
