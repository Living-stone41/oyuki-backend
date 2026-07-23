package com.oyuki.review.controller;

import com.oyuki.review.dto.HideReviewRequest;
import com.oyuki.review.dto.ReviewResponse;
import com.oyuki.review.enums.ReviewStatus;
import com.oyuki.review.enums.ReviewType;
import com.oyuki.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(
            ReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>>
    getReviews(
            Authentication authentication,

            @RequestParam(required = false)
            ReviewType reviewType,

            @RequestParam(required = false)
            ReviewStatus status
    ) {
        return ResponseEntity.ok(
                reviewService.getAdminReviews(
                        getUserId(authentication),
                        reviewType,
                        status
                )
        );
    }

    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<ReviewResponse>
    hideReview(
            Authentication authentication,
            @PathVariable Long reviewId,
            @Valid @RequestBody
            HideReviewRequest request
    ) {
        return ResponseEntity.ok(
                reviewService.hideReview(
                        getUserId(authentication),
                        reviewId,
                        request
                )
        );
    }

    @PatchMapping("/{reviewId}/restore")
    public ResponseEntity<ReviewResponse>
    restoreReview(
            Authentication authentication,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(
                reviewService.restoreReview(
                        getUserId(authentication),
                        reviewId
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}