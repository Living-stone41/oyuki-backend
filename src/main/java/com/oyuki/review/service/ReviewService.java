package com.oyuki.review.service;

import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.repository.OrderItemRepository;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.product.entity.Product;
import com.oyuki.product.repository.ProductRepository;
import com.oyuki.review.dto.CreateReviewRequest;
import com.oyuki.review.dto.HideReviewRequest;
import com.oyuki.review.dto.RatingSummaryResponse;
import com.oyuki.review.dto.ReviewResponse;
import com.oyuki.review.dto.UpdateReviewRequest;
import com.oyuki.review.entity.Review;
import com.oyuki.review.enums.ReviewStatus;
import com.oyuki.review.enums.ReviewType;
import com.oyuki.review.repository.ReviewRepository;
import com.oyuki.tracking.entity.OrderDelivery;
import com.oyuki.tracking.enums.OrderDeliveryStatus;
import com.oyuki.tracking.repository.OrderDeliveryRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderDeliveryRepository orderDeliveryRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderDeliveryRepository = orderDeliveryRepository;
    }

    /*
     * =========================================================
     * CUSTOMER CREATES PRODUCT REVIEW
     * =========================================================
     */

    @Transactional
    public ReviewResponse createProductReview(
            Long customerId,
            Long productId,
            CreateReviewRequest request
    ) {
        User customer = getActiveCustomer(customerId);

        validateCreateRequest(
                request,
                true
        );

        Order order =
                getDeliveredCustomerOrder(
                        customerId,
                        request.orderId()
                );

        OrderItem orderItem =
                getOrderItemFromOrder(
                        order.getId(),
                        request.orderItemId()
                );

        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                )
                        );

        if (
                orderItem.getProduct() == null ||
                !orderItem.getProduct()
                        .getId()
                        .equals(productId)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This product was not purchased in the selected order item"
            );
        }

        boolean alreadyReviewed =
                reviewRepository
                        .existsByCustomer_IdAndOrder_IdAndReviewTypeAndReviewedProduct_Id(
                                customerId,
                                order.getId(),
                                ReviewType.PRODUCT,
                                productId
                        );

        if (alreadyReviewed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already reviewed this product for this order"
            );
        }

        Review review =
                Review.builder()
                        .customer(customer)
                        .order(order)
                        .orderItem(orderItem)
                        .reviewType(ReviewType.PRODUCT)
                        .reviewedProduct(product)
                        .reviewedUser(null)
                        .rating(request.rating())
                        .comment(clean(request.comment()))
                        .status(ReviewStatus.VISIBLE)
                        .build();

        return ReviewResponse.from(
                reviewRepository.save(review)
        );
    }

    /*
     * =========================================================
     * CUSTOMER CREATES SELLER OR KITCHEN REVIEW
     * =========================================================
     */

    @Transactional
    public ReviewResponse createProviderReview(
            Long customerId,
            Long providerId,
            CreateReviewRequest request
    ) {
        User customer = getActiveCustomer(customerId);

        validateCreateRequest(
                request,
                true
        );

        Order order =
                getDeliveredCustomerOrder(
                        customerId,
                        request.orderId()
                );

        OrderItem orderItem =
                getOrderItemFromOrder(
                        order.getId(),
                        request.orderItemId()
                );

        User provider =
                userRepository
                        .findById(providerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Provider not found"
                                )
                        );

        ReviewType reviewType =
                getProviderReviewType(provider);

        if (
                orderItem.getOwner() == null ||
                !orderItem.getOwner()
                        .getId()
                        .equals(providerId)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This provider did not supply the selected order item"
            );
        }

        boolean alreadyReviewed =
                reviewRepository
                        .existsByCustomer_IdAndOrder_IdAndReviewTypeAndReviewedUser_Id(
                                customerId,
                                order.getId(),
                                reviewType,
                                providerId
                        );

        if (alreadyReviewed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already reviewed this provider for this order"
            );
        }

        Review review =
                Review.builder()
                        .customer(customer)
                        .order(order)
                        .orderItem(orderItem)
                        .reviewType(reviewType)
                        .reviewedProduct(null)
                        .reviewedUser(provider)
                        .rating(request.rating())
                        .comment(clean(request.comment()))
                        .status(ReviewStatus.VISIBLE)
                        .build();

        return ReviewResponse.from(
                reviewRepository.save(review)
        );
    }

    /*
     * =========================================================
     * CUSTOMER CREATES RIDER REVIEW
     * =========================================================
     */

    @Transactional
    public ReviewResponse createRiderReview(
            Long customerId,
            Long riderId,
            CreateReviewRequest request
    ) {
        User customer = getActiveCustomer(customerId);

        validateCreateRequest(
                request,
                false
        );

        Order order =
                getDeliveredCustomerOrder(
                        customerId,
                        request.orderId()
                );

        User rider =
                userRepository
                        .findById(riderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Rider not found"
                                )
                        );

        if (rider.getRole() != Role.RIDER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected user is not a rider"
            );
        }

        OrderDelivery delivery =
                orderDeliveryRepository
                        .findByOrder_IdAndOrder_Customer_Id(
                                order.getId(),
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Delivery information was not found for this order"
                                )
                        );

        if (
                delivery.getStatus()
                        != OrderDeliveryStatus.DELIVERED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The delivery must be completed before reviewing the rider"
            );
        }

        if (
                delivery.getRider() == null ||
                !delivery.getRider()
                        .getId()
                        .equals(riderId)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This rider did not deliver the selected order"
            );
        }

        boolean alreadyReviewed =
                reviewRepository
                        .existsByCustomer_IdAndOrder_IdAndReviewTypeAndReviewedUser_Id(
                                customerId,
                                order.getId(),
                                ReviewType.RIDER,
                                riderId
                        );

        if (alreadyReviewed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already reviewed this rider for this order"
            );
        }

        Review review =
                Review.builder()
                        .customer(customer)
                        .order(order)
                        .orderItem(null)
                        .reviewType(ReviewType.RIDER)
                        .reviewedProduct(null)
                        .reviewedUser(rider)
                        .rating(request.rating())
                        .comment(clean(request.comment()))
                        .status(ReviewStatus.VISIBLE)
                        .build();

        return ReviewResponse.from(
                reviewRepository.save(review)
        );
    }

    /*
     * =========================================================
     * CUSTOMER REVIEW MANAGEMENT
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(
            Long customerId
    ) {
        getActiveCustomer(customerId);

        return reviewRepository
                .findAllByCustomer_IdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse updateReview(
            Long customerId,
            Long reviewId,
            UpdateReviewRequest request
    ) {
        getActiveCustomer(customerId);

        if (
                request == null ||
                request.rating() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating is required"
            );
        }

        validateRating(request.rating());

        Review review =
                reviewRepository
                        .findByIdAndCustomer_Id(
                                reviewId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Review not found"
                                )
                        );

        review.setRating(request.rating());
        review.setComment(clean(request.comment()));

        return ReviewResponse.from(
                reviewRepository.save(review)
        );
    }

    @Transactional
    public void deleteReview(
            Long customerId,
            Long reviewId
    ) {
        getActiveCustomer(customerId);

        Review review =
                reviewRepository
                        .findByIdAndCustomer_Id(
                                reviewId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Review not found"
                                )
                        );

        reviewRepository.delete(review);
    }

    /*
     * =========================================================
     * PUBLIC PRODUCT REVIEWS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(
            Long productId
    ) {
        productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );

        return reviewRepository
                .findAllByReviewedProduct_IdAndStatusOrderByCreatedAtDesc(
                        productId,
                        ReviewStatus.VISIBLE
                )
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getProductRatingSummary(
            Long productId
    ) {
        productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );

        List<Review> reviews =
                reviewRepository
                        .findAllByReviewedProduct_IdAndStatus(
                                productId,
                                ReviewStatus.VISIBLE
                        );

        return buildSummary(reviews);
    }

    /*
     * =========================================================
     * PUBLIC PROVIDER REVIEWS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProviderReviews(
            Long providerId
    ) {
        User provider =
                userRepository
                        .findById(providerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Provider not found"
                                )
                        );

        ReviewType reviewType =
                getProviderReviewType(provider);

        return reviewRepository
                .findAllByReviewedUser_IdAndReviewTypeAndStatusOrderByCreatedAtDesc(
                        providerId,
                        reviewType,
                        ReviewStatus.VISIBLE
                )
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getProviderRatingSummary(
            Long providerId
    ) {
        User provider =
                userRepository
                        .findById(providerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Provider not found"
                                )
                        );

        ReviewType reviewType =
                getProviderReviewType(provider);

        List<Review> reviews =
                reviewRepository
                        .findAllByReviewedUser_IdAndReviewTypeAndStatus(
                                providerId,
                                reviewType,
                                ReviewStatus.VISIBLE
                        );

        return buildSummary(reviews);
    }

    /*
     * =========================================================
     * PUBLIC RIDER REVIEWS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<ReviewResponse> getRiderReviews(
            Long riderId
    ) {
        getRider(riderId);

        return reviewRepository
                .findAllByReviewedUser_IdAndReviewTypeAndStatusOrderByCreatedAtDesc(
                        riderId,
                        ReviewType.RIDER,
                        ReviewStatus.VISIBLE
                )
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getRiderRatingSummary(
            Long riderId
    ) {
        getRider(riderId);

        List<Review> reviews =
                reviewRepository
                        .findAllByReviewedUser_IdAndReviewTypeAndStatus(
                                riderId,
                                ReviewType.RIDER,
                                ReviewStatus.VISIBLE
                        );

        return buildSummary(reviews);
    }

    /*
     * =========================================================
     * ADMIN MODERATION
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAdminReviews(
            Long adminId,
            ReviewType reviewType,
            ReviewStatus status
    ) {
        getActiveAdmin(adminId);

        List<Review> reviews;

        if (
                reviewType == null &&
                status == null
        ) {
            reviews =
                    reviewRepository
                            .findAllByOrderByCreatedAtDesc();

        } else if (
                reviewType != null &&
                status == null
        ) {
            reviews =
                    reviewRepository
                            .findAllByReviewTypeOrderByCreatedAtDesc(
                                    reviewType
                            );

        } else if (reviewType == null) {
            reviews =
                    reviewRepository
                            .findAllByStatusOrderByCreatedAtDesc(
                                    status
                            );

        } else {
            reviews =
                    reviewRepository
                            .findAllByReviewTypeAndStatusOrderByCreatedAtDesc(
                                    reviewType,
                                    status
                            );
        }

        return reviews.stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse hideReview(
            Long adminId,
            Long reviewId,
            HideReviewRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        if (
                request == null ||
                request.reason() == null ||
                request.reason().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Moderation reason is required"
            );
        }

        Review review = getReview(reviewId);

        review.setStatus(ReviewStatus.HIDDEN);
        review.setModerationReason(
                request.reason().trim()
        );
        review.setModeratedBy(admin);
        review.setModeratedAt(
                LocalDateTime.now()
        );

        return ReviewResponse.from(
                reviewRepository.save(review)
        );
    }

    @Transactional
    public ReviewResponse restoreReview(
            Long adminId,
            Long reviewId
    ) {
        User admin = getActiveAdmin(adminId);

        Review review = getReview(reviewId);

        review.setStatus(ReviewStatus.VISIBLE);
        review.setModerationReason(null);
        review.setModeratedBy(admin);
        review.setModeratedAt(
                LocalDateTime.now()
        );

        return ReviewResponse.from(
                reviewRepository.save(review)
        );
    }

    /*
     * =========================================================
     * RATING SUMMARY
     * =========================================================
     */

    private RatingSummaryResponse buildSummary(
            List<Review> reviews
    ) {
        long total = reviews.size();

        if (total == 0) {
            return new RatingSummaryResponse(
                    0,
                    BigDecimal.ZERO,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        int ratingTotal =
                reviews.stream()
                        .map(Review::getRating)
                        .filter(rating -> rating != null)
                        .mapToInt(Integer::intValue)
                        .sum();

        BigDecimal average =
                BigDecimal.valueOf(ratingTotal)
                        .divide(
                                BigDecimal.valueOf(total),
                                2,
                                RoundingMode.HALF_UP
                        );

        return new RatingSummaryResponse(
                total,
                average,
                countRatings(reviews, 5),
                countRatings(reviews, 4),
                countRatings(reviews, 3),
                countRatings(reviews, 2),
                countRatings(reviews, 1)
        );
    }

    private long countRatings(
            List<Review> reviews,
            int rating
    ) {
        return reviews.stream()
                .filter(review ->
                        review.getRating() != null &&
                        review.getRating() == rating
                )
                .count();
    }

    /*
     * =========================================================
     * VALIDATION HELPERS
     * =========================================================
     */

    private Order getDeliveredCustomerOrder(
            Long customerId,
            Long orderId
    ) {
        Order order =
                orderRepository
                        .findByIdAndCustomer_Id(
                                orderId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        if (
                order.getStatus()
                        != OrderStatus.DELIVERED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only delivered orders can be reviewed"
            );
        }

        return order;
    }

    private OrderItem getOrderItemFromOrder(
            Long orderId,
            Long orderItemId
    ) {
        if (orderItemId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order item ID is required"
            );
        }

        OrderItem orderItem =
                orderItemRepository
                        .findById(orderItemId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order item not found"
                                )
                        );

        if (
                orderItem.getOrder() == null ||
                !orderItem.getOrder()
                        .getId()
                        .equals(orderId)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The order item does not belong to the selected order"
            );
        }

        return orderItem;
    }

    private ReviewType getProviderReviewType(
            User provider
    ) {
        if (provider.getRole() == Role.SELLER) {
            return ReviewType.SELLER;
        }

        if (provider.getRole() == Role.KITCHEN) {
            return ReviewType.KITCHEN;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The selected user is not a seller or kitchen"
        );
    }

    private User getRider(
            Long riderId
    ) {
        User rider =
                userRepository
                        .findById(riderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Rider not found"
                                )
                        );

        if (rider.getRole() != Role.RIDER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected user is not a rider"
            );
        }

        return rider;
    }

    private User getActiveCustomer(
            Long customerId
    ) {
        User customer =
                userRepository
                        .findById(customerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer account not found"
                                )
                        );

        if (customer.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only customers can manage reviews"
            );
        }

        if (
                customer.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Customer account is not active"
            );
        }

        return customer;
    }

    private User getActiveAdmin(
            Long adminId
    ) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Administrator account not found"
                                )
                        );

        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can moderate reviews"
            );
        }

        if (
                admin.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator account is not active"
            );
        }

        return admin;
    }

    private Review getReview(
            Long reviewId
    ) {
        return reviewRepository
                .findById(reviewId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Review not found"
                        )
                );
    }

    private void validateCreateRequest(
            CreateReviewRequest request,
            boolean orderItemRequired
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Review request is required"
            );
        }

        if (request.orderId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order ID is required"
            );
        }

        if (
                orderItemRequired &&
                request.orderItemId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order item ID is required"
            );
        }

        if (request.rating() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating is required"
            );
        }

        validateRating(request.rating());
    }

    private void validateRating(
            Integer rating
    ) {
        if (
                rating == null ||
                rating < 1 ||
                rating > 5
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be between 1 and 5"
            );
        }
    }

    private String clean(
            String value
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}