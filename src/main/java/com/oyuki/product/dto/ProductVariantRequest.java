package com.oyuki.product.dto;

import com.oyuki.product.enums.MeasurementUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantRequest(

        @NotNull(message = "Measurement value is required")
        @DecimalMin(
                value = "0.001",
                message = "Measurement value must be greater than zero"
        )
        BigDecimal measurementValue,

        @NotNull(message = "Measurement unit is required")
        MeasurementUnit measurementUnit,

        @NotNull(message = "Price is required")
        @DecimalMin(
                value = "0.01",
                message = "Price must be greater than zero"
        )
        BigDecimal price,

        @Min(
                value = 0,
                message = "Stock cannot be negative"
        )
        int stockQuantity,

        @Min(
                value = 1,
                message = "Minimum order quantity must be at least one"
        )
        int minimumOrderQuantity,

        @Size(
                max = 100,
                message = "SKU cannot exceed 100 characters"
        )
        String sku,

        Boolean available
) {
}