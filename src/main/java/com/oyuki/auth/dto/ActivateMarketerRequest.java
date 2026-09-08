package com.oyuki.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActivateMarketerRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Email OTP is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Email OTP must contain 6 digits")
        String emailOtp,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must contain at least 8 characters")
        String password,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {}
