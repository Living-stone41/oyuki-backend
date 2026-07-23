package com.oyuki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPasswordResetRequest(

        @NotBlank(message = "Email address or phone number is required")
        String contact,

        @NotBlank(message = "Reset code is required")
        @Pattern(
                regexp = "^[0-9]{6}$",
                message = "Reset code must contain 6 digits"
        )
        String token
) {
}
