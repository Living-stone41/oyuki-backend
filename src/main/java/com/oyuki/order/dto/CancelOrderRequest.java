package com.oyuki.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(

        @NotBlank(message = "Cancellation reason is required")
        @Size(
                max = 1000,
                message = "Cancellation reason cannot exceed 1000 characters"
        )
        String reason,

        /*
         * For paid orders:
         * true or null creates a pending full refund.
         * false cancels without creating the refund immediately.
         */
        Boolean createRefund,

        @Size(
                max = 1000,
                message = "Admin note cannot exceed 1000 characters"
        )
        String note

) {
}