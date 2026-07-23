package com.oyuki.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(

        @Min(
                value = 1,
                message = "Quantity must be at least one"
        )
        int quantity

) {
}