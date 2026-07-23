package com.oyuki.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailRefundRequest(

        @NotBlank(
                message = "Failure reason is required"
        )
        @Size(
                max = 1000,
                message = "Failure reason cannot exceed 1000 characters"
        )
        String reason

) {
}