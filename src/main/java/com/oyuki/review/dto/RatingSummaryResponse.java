package com.oyuki.review.dto;

import java.math.BigDecimal;

public record RatingSummaryResponse(

        long totalReviews,

        BigDecimal averageRating,

        long fiveStarReviews,
        long fourStarReviews,
        long threeStarReviews,
        long twoStarReviews,
        long oneStarReviews

) {
}