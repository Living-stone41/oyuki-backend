package com.oyuki.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteRefundRequest(

        @NotBlank(
                message = "Refund transaction reference is required"
        )
        @Size(
                max = 200,
                message = "Transaction reference cannot exceed 200 characters"
        )
        String transactionReference,

        @Size(
                max = 1000,
                message = "Admin note cannot exceed 1000 characters"
        )
        String note

) {
}