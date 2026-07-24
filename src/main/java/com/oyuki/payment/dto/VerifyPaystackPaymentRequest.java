package com.oyuki.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPaystackPaymentRequest(
        @NotBlank(message = "Paystack transaction reference is required")
        String reference
) {
}
