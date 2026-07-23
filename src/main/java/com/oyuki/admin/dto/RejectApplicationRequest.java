package com.oyuki.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectApplicationRequest(

        @NotBlank(message = "Rejection reason is required")
        @Size(
                max = 500,
                message = "Rejection reason cannot exceed 500 characters"
        )
        String reason
) {
}