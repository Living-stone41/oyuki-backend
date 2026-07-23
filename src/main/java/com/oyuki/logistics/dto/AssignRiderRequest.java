package com.oyuki.logistics.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AssignRiderRequest(

        @NotNull(message = "Rider is required")
        Long riderId,

        @NotNull(message = "Delivery fee is required")
        @DecimalMin(
                value = "0.00",
                message = "Delivery fee cannot be negative"
        )
        BigDecimal deliveryFee,

        @Size(
                max = 1000,
                message = "Delivery note cannot exceed 1000 characters"
        )
        String deliveryNote

) {
}