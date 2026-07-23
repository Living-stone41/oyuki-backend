package com.oyuki.payment.dto;

import jakarta.validation.constraints.Size;

public record ConfirmPaymentProofRequest(

        @Size(
                max = 1000,
                message = "Admin note cannot exceed 1000 characters"
        )
        String note

) {
}