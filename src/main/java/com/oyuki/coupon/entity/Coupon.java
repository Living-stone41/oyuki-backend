package com.oyuki.coupon.entity;

import com.oyuki.coupon.enums.DiscountType;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupon_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_coupon_code",
                        columnList = "code"
                ),
                @Index(
                        name = "idx_coupon_active",
                        columnList = "active"
                ),
                @Index(
                        name = "idx_coupon_dates",
                        columnList = "starts_at,expires_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "code",
            nullable = false,
            length = 40,
            unique = true
    )
    private String code;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "discount_type",
            nullable = false,
            length = 40
    )
    private DiscountType discountType;

    /*
     * PERCENTAGE: percentage value, e.g. 10.
     * FIXED_AMOUNT: naira value, e.g. 500.
     * FREE_DELIVERY: stored as zero.
     */
    @Column(
            name = "discount_value",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    /*
     * Mainly used to cap percentage discounts.
     */
    @Column(
            name = "maximum_discount_amount",
            precision = 15,
            scale = 2
    )
    private BigDecimal maximumDiscountAmount;

    @Column(
            name = "minimum_order_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    /*
     * Null means there is no global usage limit.
     */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /*
     * Null means there is no per-customer limit.
     */
    @Column(name = "per_customer_limit")
    private Integer perCustomerLimit;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(
            name = "active",
            nullable = false
    )
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_coupon_created_by"
            )
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by_id",
            foreignKey = @ForeignKey(
                    name = "fk_coupon_updated_by"
            )
    )
    private User updatedBy;

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
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (discountValue == null) {
            discountValue = BigDecimal.ZERO;
        }

        if (minimumOrderAmount == null) {
            minimumOrderAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
