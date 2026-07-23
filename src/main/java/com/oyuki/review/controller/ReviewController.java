package com.oyuki.review.controller;

import com.oyuki.review.dto.CreateReviewRequest;
import com.oyuki.review.dto.RatingSummaryResponse;
import com.oyuki.review.dto.ReviewResponse;
import com.oyuki.review.dto.UpdateReviewRequest;
import com.oyuki.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
            ReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    /*
     * CUSTOMER CREATES REVIEWS
     */

    @PostMapping("/products/{productId}")
    public ResponseEntity<ReviewResponse>
    createProductReview(
            Authentication authentication,
            @PathVariable Long productId,
            @Valid @RequestBody
            CreateReviewRequest request
    ) {
        return ResponseEntity.ok(
                reviewService.createProductReview(
                        getUserId(authentication),
                        productId,
                        request
                )
        );
    }

    @PostMapping("/providers/{providerId}")
    public ResponseEntity<ReviewResponse>
    createProviderReview(
            Authentication authentication,
            @PathVariable Long providerId,
            @Valid @RequestBody
            CreateReviewRequest request
    ) {
        return ResponseEntity.ok(
                reviewService.createProviderReview(
                        getUserId(authentication),
                        providerId,
                        request
                )
        );
    }

    @PostMapping("/riders/{riderId}")
    public ResponseEntity<ReviewResponse>
    createRiderReview(
            Authentication authentication,
            @PathVariable Long riderId,
            @Valid @RequestBody
            CreateReviewRequest request
    ) {
        return ResponseEntity.ok(
                reviewService.createRiderReview(
                        getUserId(authentication),
                        riderId,
                        request
                )
        );
    }

    /*
     * CUSTOMER MANAGES REVIEWS
     */

    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>>
    getMyReviews(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                reviewService.getMyReviews(
                        getUserId(authentication)
                )
        );
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse>
    updateReview(
            Authentication authentication,
            @PathVariable Long reviewId,
            @Valid @RequestBody
            UpdateReviewRequest request
    ) {
        return ResponseEntity.ok(
                reviewService.updateReview(
                        getUserId(authentication),
                        reviewId,
                        request
                )
        );
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void>
    deleteReview(
            Authentication authentication,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(
                getUserId(authentication),
                reviewId
        );

        return ResponseEntity.noContent().build();
    }

    /*
     * PUBLIC PRODUCT REVIEWS
     */

    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ReviewResponse>>
    getProductReviews(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                reviewService.getProductReviews(
                        productId
                )
        );
    }

    @GetMapping("/products/{productId}/summary")
    public ResponseEntity<RatingSummaryResponse>
    getProductSummary(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                reviewService
                        .getProductRatingSummary(
                                productId
                        )
        );
    }

    /*
     * PUBLIC PROVIDER REVIEWS
     */

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<List<ReviewResponse>>
    getProviderReviews(
            @PathVariable Long providerId
    ) {
        return ResponseEntity.ok(
                reviewService.getProviderReviews(
                        providerId
                )
        );
    }

    @GetMapping("/providers/{providerId}/summary")
    public ResponseEntity<RatingSummaryResponse>
    getProviderSummary(
            @PathVariable Long providerId
    ) {
        return ResponseEntity.ok(
                reviewService
                        .getProviderRatingSummary(
                                providerId
                        )
        );
    }

    /*
     * PUBLIC RIDER REVIEWS
     */

    @GetMapping("/riders/{riderId}")
    public ResponseEntity<List<ReviewResponse>>
    getRiderReviews(
            @PathVariable Long riderId
    ) {
        return ResponseEntity.ok(
                reviewService.getRiderReviews(
                        riderId
                )
        );
    }

    @GetMapping("/riders/{riderId}/summary")
    public ResponseEntity<RatingSummaryResponse>
    getRiderSummary(
            @PathVariable Long riderId
    ) {
        return ResponseEntity.ok(
                reviewService
                        .getRiderRatingSummary(
                                riderId
                        )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}