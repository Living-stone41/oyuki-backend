package com.oyuki.newsletter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendNewsletterRequest(
        @NotBlank @Size(max = 180) String subject,
        @NotBlank @Size(max = 10000) String message
) {}
