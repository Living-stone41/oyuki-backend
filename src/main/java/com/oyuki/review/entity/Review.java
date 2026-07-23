package com.oyuki.review.entity;

import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.product.entity.Product;
import com.oyuki.review.enums.ReviewStatus;
import com.oyuki.review.enums.ReviewType;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(
                        name = "idx_review_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_review_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_review_product",
                        columnList = "reviewed_product_id"
                ),
                @Index(
                        name = "idx_review_user",
                        columnList = "reviewed_user_id"
                ),
                @Index(
                        name = "idx_review_type",
                        columnList = "review_type"
                ),
                @Index(
                        name = "idx_review_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Customer who submitted the review.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_review_customer"
            )
    )
    private User customer;

    /*
     * Delivered order that gave the customer
     * permission to submit the review.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_review_order"
            )
    )
    private Order order;

    /*
     * Used for product, seller and kitchen reviews.
     *
     * It proves that the reviewed item was part
     * of the customer's delivered order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "order_item_id",
            foreignKey = @ForeignKey(
                    name = "fk_review_order_item"
            )
    )
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "review_type",
            nullable = false,
            length = 30
    )
    private ReviewType reviewType;

    /*
     * Filled only when reviewType is PRODUCT.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reviewed_product_id",
            foreignKey = @ForeignKey(
                    name = "fk_review_product"
            )
    )
    private Product reviewedProduct;

    /*
     * Filled for SELLER, KITCHEN or RIDER reviews.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reviewed_user_id",
            foreignKey = @ForeignKey(
                    name = "fk_review_user"
            )
    )
    private User reviewedUser;

    @Column(
            name = "rating",
            nullable = false
    )
    private Integer rating;

    @Column(
            name = "comment",
            length = 2000
    )
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private ReviewStatus status =
            ReviewStatus.VISIBLE;

    /*
     * Optional reason entered when an admin
     * hides the review.
     */
    @Column(
            name = "moderation_reason",
            length = 1000
    )
    private String moderationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "moderated_by_id",
            foreignKey = @ForeignKey(
                    name = "fk_review_moderated_by"
            )
    )
    private User moderatedBy;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = ReviewStatus.VISIBLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}