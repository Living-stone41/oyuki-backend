package com.oyuki.delivery.dto;

import jakarta.validation.constraints.NotNull;

public record DeliveryFeeRequest(

        @NotNull(message = "Delivery address ID is required")
        Long addressId

) {
}
