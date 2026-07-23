package com.oyuki.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContactMessageRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Email address is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 180, message = "Email address is too long")
        String email,

        @NotBlank(message = "Subject is required")
        @Size(max = 180, message = "Subject must not exceed 180 characters")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 4000, message = "Message must be between 10 and 4000 characters")
        String message
) {
}
