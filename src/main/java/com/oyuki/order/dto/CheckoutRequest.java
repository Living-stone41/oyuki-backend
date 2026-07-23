package com.oyuki.order.dto;

import com.oyuki.order.enums.DeliveryType;
import com.oyuki.order.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(

        /*
         * When omitted, CUSTOMER_ADDRESS is used.
         */
        DeliveryType deliveryType,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        /*
         * Required for CUSTOMER_ADDRESS checkout.
         */
        Long addressId,

        /*
         * Required for KITCHEN_ADDRESS checkout.
         */
        Long destinationKitchenId,

        /*
         * Optional. Leave null or blank when the
         * customer is not applying a coupon.
         */
        @Pattern(
                regexp = "^[A-Za-z0-9_-]{3,40}$",
                message = "Enter a valid coupon code"
        )
        String couponCode,

        /*
         * The fields below are used for KITCHEN_ADDRESS.
         * For CUSTOMER_ADDRESS, the backend copies the
         * details from the customer's saved address.
         */
        @Size(max = 150)
        String recipientName,

        @Size(max = 30)
        String recipientPhone,

        @Size(max = 100)
        String state,

        @Size(max = 100)
        String lga,

        @Size(max = 150)
        String area,

        @Size(max = 500)
        String addressLine,

        /*
         * For CUSTOMER_ADDRESS, this overrides the saved
         * delivery instructions when provided.
         */
        @Size(max = 1000)
        String deliveryInstructions

) {
}
