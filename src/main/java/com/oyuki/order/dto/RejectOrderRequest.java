package com.oyuki.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectOrderRequest(

        @NotBlank(message = "Rejection reason is required")
        @Size(
                min = 5,
                max = 1000,
                message = "Rejection reason must contain between 5 and 1000 characters"
        )
        String reason

) {
}