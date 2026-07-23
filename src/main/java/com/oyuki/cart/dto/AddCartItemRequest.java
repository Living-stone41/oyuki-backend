package com.oyuki.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(

        @NotNull(message = "Product variant is required")
        Long variantId,

        @Min(
                value = 1,
                message = "Quantity must be at least one"
        )
        int quantity

) {
}