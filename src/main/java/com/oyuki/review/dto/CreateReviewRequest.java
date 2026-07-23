package com.oyuki.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(

        /*
         * Delivered order containing the product,
         * provider or rider being reviewed.
         */
        @NotNull(
                message = "Order ID is required"
        )
        Long orderId,

        /*
         * Required for product, seller and kitchen
         * reviews. Not required for rider reviews.
         */
        Long orderItemId,

        @NotNull(
                message = "Rating is required"
        )
        @Min(
                value = 1,
                message = "Rating must be at least 1"
        )
        @Max(
                value = 5,
                message = "Rating cannot exceed 5"
        )
        Integer rating,

        @Size(
                max = 2000,
                message = "Review comment cannot exceed 2000 characters"
        )
        String comment

) {
}