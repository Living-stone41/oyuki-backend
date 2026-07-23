package com.oyuki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Email address or phone number is required")
        String contact,

        @NotBlank(message = "Reset code is required")
        @Pattern(
                regexp = "^[0-9]{6}$",
                message = "Reset code must contain 6 digits"
        )
        String token,

        @NotBlank(message = "New password is required")
        @Size(
                min = 8,
                max = 100,
                message = "New password must contain at least 8 characters"
        )
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {
}