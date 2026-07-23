package com.oyuki.tracking.dto;

import jakarta.validation.constraints.Size;

public record UpdateDeliveryNoteRequest(

        @Size(
                max = 1000,
                message = "Rider note cannot exceed 1000 characters"
        )
        String note

) {
}