package com.oyuki.admin.controller;

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

@RestController
@RequestMapping("/api/admin/marketers")
@RequiredArgsConstructor
public class AdminMarketerController {

    private final AdminMarketerService service;

    /**
     * Creates a new marketer account.
     *
     * Marketers are created by an administrator and cannot
     * register themselves through the public registration page.
     *
     * The marketer receives an OTP and must complete account
     * verification before the account becomes active.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminMarketerResponse>> create(
            @Valid @RequestBody CreateMarketerRequest request
    ) {

        AdminMarketerResponse marketer = service.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Marketer created. An OTP has been sent for account activation.",
                                marketer
                        )
                );
    }

    /**
     * Returns all marketer accounts.
     */
    @GetMapping
    public ApiResponse<List<AdminMarketerResponse>> list() {

        return ApiResponse.success(
                "Marketers retrieved successfully",
                service.list()
        );
    }
}