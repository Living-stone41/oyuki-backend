package com.oyuki.review.dto;

import com.oyuki.review.entity.Review;
import com.oyuki.review.enums.ReviewStatus;
import com.oyuki.review.enums.ReviewType;

import java.time.LocalDateTime;

public record ReviewResponse(

        Long id,

        ReviewType reviewType,
        ReviewStatus status,

        Integer rating,
        String comment,

        Long customerId,
        String customerName,

        Long orderId,
        String orderNumber,

        Long orderItemId,

        Long reviewedProductId,
        String reviewedProductName,

        Long reviewedUserId,
        String reviewedUserName,
        String reviewedUserRole,

        String moderationReason,

        Long moderatedById,
        String moderatedByName,
        LocalDateTime moderatedAt,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static ReviewResponse from(
            Review review
    ) {
        return new ReviewResponse(
                review.getId(),

                review.getReviewType(),
                review.getStatus(),

                review.getRating(),
                review.getComment(),

                review.getCustomer().getId(),
                review.getCustomer().getFullName(),

                review.getOrder().getId(),
                review.getOrder().getOrderNumber(),

                review.getOrderItem() == null
                        ? null
                        : review.getOrderItem().getId(),

                review.getReviewedProduct() == null
                        ? null
                        : review.getReviewedProduct().getId(),

                review.getReviewedProduct() == null
                        ? null
                        : review.getReviewedProduct().getName(),

                review.getReviewedUser() == null
                        ? null
                        : review.getReviewedUser().getId(),

                review.getReviewedUser() == null
                        ? null
                        : review.getReviewedUser().getFullName(),

                review.getReviewedUser() == null ||
                review.getReviewedUser().getRole() == null
                        ? null
                        : review.getReviewedUser()
                                .getRole()
                                .name(),

                review.getModerationReason(),

                review.getModeratedBy() == null
                        ? null
                        : review.getModeratedBy().getId(),

                review.getModeratedBy() == null
                        ? null
                        : review.getModeratedBy().getFullName(),

                review.getModeratedAt(),

                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}