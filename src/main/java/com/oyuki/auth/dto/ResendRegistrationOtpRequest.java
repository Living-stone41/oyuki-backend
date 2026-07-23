package com.oyuki.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendRegistrationOtpRequest(
        @NotBlank(message = "Email or phone number is required")
        String contact
) {}
