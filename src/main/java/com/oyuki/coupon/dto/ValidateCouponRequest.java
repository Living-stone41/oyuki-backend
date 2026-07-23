package com.oyuki.coupon.dto;

import com.oyuki.order.enums.DeliveryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ValidateCouponRequest(

        @NotBlank(message = "Coupon code is required")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]{3,40}$",
                message = "Enter a valid coupon code"
        )
        String code,

        /*
         * Defaults to CUSTOMER_ADDRESS when null.
         */
        DeliveryType deliveryType,

        Long addressId,

        Long destinationKitchenId

) {
}
