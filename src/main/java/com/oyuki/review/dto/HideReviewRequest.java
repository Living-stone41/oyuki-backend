package com.oyuki.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HideReviewRequest(

        @NotBlank(message = "Moderation reason is required")
        @Size(
                max = 1000,
                message = "Moderation reason cannot exceed 1000 characters"
        )
        String reason

) {
}