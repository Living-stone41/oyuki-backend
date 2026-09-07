package com.oyuki.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMarketAgentRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150)
        String fullName,

        @Email(message = "Enter a valid email address")
        String email,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Enter a valid phone number")
        String phoneNumber,

        @NotNull(message = "State is required")
        Long stateId,

        @NotNull(message = "LGA is required")
        Long lgaId,

        @NotNull(message = "Market is required")
        Long marketId,

        @Size(max = 150)
        String emergencyContactName,

        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "Enter a valid emergency contact phone number")
        String emergencyContactPhone
) {}
