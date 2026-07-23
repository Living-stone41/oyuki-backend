package com.oyuki.refund.dto;

import com.oyuki.refund.enums.RefundType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRefundRequest(

        @NotNull(
                message = "Refund type is required"
        )
        RefundType refundType,

        @NotNull(
                message = "Refund amount is required"
        )
        @DecimalMin(
                value = "0.01",
                message = "Refund amount must be greater than zero"
        )
        BigDecimal amount,

        @NotBlank(
                message = "Refund reason is required"
        )
        @Size(
                max = 1000,
                message = "Refund reason cannot exceed 1000 characters"
        )
        String reason,

        @Size(
                max = 1000,
                message = "Admin note cannot exceed 1000 characters"
        )
        String note

) {
}