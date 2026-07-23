package com.oyuki.review.repository;

import com.oyuki.review.entity.Review;
import com.oyuki.review.enums.ReviewStatus;
import com.oyuki.review.enums.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository
        extends JpaRepository<Review, Long> {

    /*
     * Customer's own review history.
     */
    List<Review>
    findAllByCustomer_IdOrderByCreatedAtDesc(
            Long customerId
    );

    Optional<Review>
    findByIdAndCustomer_Id(
            Long reviewId,
            Long customerId
    );

    /*
     * Public product reviews.
     */
    List<Review>
    findAllByReviewedProduct_IdAndStatusOrderByCreatedAtDesc(
            Long productId,
            ReviewStatus status
    );

    /*
     * Public seller, kitchen or rider reviews.
     */
    List<Review>
    findAllByReviewedUser_IdAndStatusOrderByCreatedAtDesc(
            Long userId,
            ReviewStatus status
    );

    List<Review>
    findAllByReviewedUser_IdAndReviewTypeAndStatusOrderByCreatedAtDesc(
            Long userId,
            ReviewType reviewType,
            ReviewStatus status
    );

    /*
     * Admin review list.
     */
    List<Review>
    findAllByOrderByCreatedAtDesc();

    List<Review>
    findAllByStatusOrderByCreatedAtDesc(
            ReviewStatus status
    );

    List<Review>
    findAllByReviewTypeOrderByCreatedAtDesc(
            ReviewType reviewType
    );

    List<Review>
    findAllByReviewTypeAndStatusOrderByCreatedAtDesc(
            ReviewType reviewType,
            ReviewStatus status
    );

    /*
     * Prevent duplicate product reviews for
     * the same order.
     */
    boolean existsByCustomer_IdAndOrder_IdAndReviewTypeAndReviewedProduct_Id(
            Long customerId,
            Long orderId,
            ReviewType reviewType,
            Long productId
    );

    /*
     * Prevent duplicate seller, kitchen or rider
     * reviews for the same order.
     */
    boolean existsByCustomer_IdAndOrder_IdAndReviewTypeAndReviewedUser_Id(
            Long customerId,
            Long orderId,
            ReviewType reviewType,
            Long reviewedUserId
    );

    /*
     * Used when calculating rating summaries.
     */
    List<Review>
    findAllByReviewedProduct_IdAndStatus(
            Long productId,
            ReviewStatus status
    );

    List<Review>
    findAllByReviewedUser_IdAndReviewTypeAndStatus(
            Long userId,
            ReviewType reviewType,
            ReviewStatus status
    );
}