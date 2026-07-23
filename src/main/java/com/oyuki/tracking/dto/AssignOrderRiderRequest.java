package com.oyuki.tracking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignOrderRiderRequest(

        @NotNull(
                message = "Rider ID is required"
        )
        Long riderId,

        @Size(
                max = 1000,
                message = "Admin note cannot exceed 1000 characters"
        )
        String note

) {
}