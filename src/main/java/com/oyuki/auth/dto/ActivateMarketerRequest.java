package com.oyuki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActivateMarketerRequest(
        @NotBlank(message = "Email or phone number is required") String contact,
        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must contain 6 digits") String token,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must contain at least 8 characters") String password,
        @NotBlank(message = "Confirm password is required") String confirmPassword
) {}
