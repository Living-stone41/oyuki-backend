package com.oyuki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyRegistrationRequest(

        @NotBlank(message = "Email or phone number is required")
        String contact,

        @NotBlank(message = "Verification code is required")
        @Pattern(
                regexp = "^[0-9]{6}$",
                message = "Verification code must contain 6 digits"
        )
        String token
) {
}