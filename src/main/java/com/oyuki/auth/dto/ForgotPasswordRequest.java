package com.oyuki.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(

        @NotBlank(message = "Email address or phone number is required")
        String contact
) {
}