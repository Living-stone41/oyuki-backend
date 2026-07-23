package com.oyuki.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPaymentRequest(

        @NotBlank(
                message = "Transaction reference is required"
        )
        @Size(
                max = 150,
                message = "Transaction reference cannot exceed 150 characters"
        )
        String transactionReference,

        @Size(
                max = 1000,
                message = "Payment note cannot exceed 1000 characters"
        )
        String note

) {
}