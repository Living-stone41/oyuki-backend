package com.oyuki.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPaymentRequest(

        @NotBlank(
                message = "Payment rejection reason is required"
        )
        @Size(
                max = 1000,
                message = "Rejection reason cannot exceed 1000 characters"
        )
        String reason

) {
}