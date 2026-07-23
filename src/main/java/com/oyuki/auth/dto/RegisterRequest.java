package com.oyuki.auth.dto;

import com.oyuki.auth.enums.OtpChannel;
import com.oyuki.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z .'\\-]{1,149}$",
                message = "Full name can contain letters, spaces, apostrophes and hyphens only"
        )
        String fullName,

        @Email(message = "Please enter a valid email address")
        String email,

        @Pattern(
                regexp = "^\\+?[0-9]{10,15}$",
                message = "Please enter a valid phone number"
        )
        String phoneNumber,

        /*
         * Optional for backward compatibility.
         * When both email and phone are supplied, this determines
         * where the registration OTP is delivered.
         */
        OtpChannel otpChannel,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must contain at least 8 characters")
        String password,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword,

        @NotNull(message = "Account role is required")
        Role role
) {
}
