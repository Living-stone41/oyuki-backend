package com.oyuki.logistics.dto;

import com.oyuki.logistics.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeliveryStatusRequest(

        @NotNull(message = "Delivery status is required")
        DeliveryStatus status,

        @Size(
                max = 1000,
                message = "Delivery note cannot exceed 1000 characters"
        )
        String note,

        @Size(
                max = 1000,
                message = "Failure reason cannot exceed 1000 characters"
        )
        String failureReason

) {
}