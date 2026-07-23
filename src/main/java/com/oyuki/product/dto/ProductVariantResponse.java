package com.oyuki.product.dto;

import com.oyuki.product.entity.ProductVariant;
import com.oyuki.product.enums.MeasurementUnit;

import java.math.BigDecimal;

public record ProductVariantResponse(

        Long id,
        BigDecimal measurementValue,
        MeasurementUnit measurementUnit,
        BigDecimal price,
        int stockQuantity,
        int minimumOrderQuantity,
        String sku,
        boolean available

) {

    public static ProductVariantResponse from(
            ProductVariant variant
    ) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getMeasurementValue(),
                variant.getMeasurementUnit(),
                variant.getPrice(),
                variant.getStockQuantity(),
                variant.getMinimumOrderQuantity(),
                variant.getSku(),
                variant.isAvailable()
        );
    }
}