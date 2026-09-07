package com.oyuki.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActivateMarketerRequest(
        String email,
        String phoneNumber,
        @Pattern(regexp = "^$|^[0-9]{6}$", message = "Email OTP must contain 6 digits") String emailOtp,
        @Pattern(regexp = "^$|^[0-9]{6}$", message = "Phone OTP must contain 6 digits") String phoneOtp,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must contain at least 8 characters") String password,
        @NotBlank(message = "Confirm password is required") String confirmPassword
) {}
