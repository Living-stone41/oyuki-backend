package com.oyuki.coupon.entity;

import com.oyuki.order.entity.Order;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupon_usages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupon_usage_order",
                        columnNames = {
                                "coupon_id",
                                "order_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_coupon_usage_coupon",
                        columnList = "coupon_id"
                ),
                @Index(
                        name = "idx_coupon_usage_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_coupon_usage_order",
                        columnList = "order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "coupon_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_coupon_usage_coupon"
            )
    )
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_coupon_usage_customer"
            )
    )
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_coupon_usage_order"
            )
    )
    private Order order;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal discountAmount;

    @Column(
            name = "used_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        if (usedAt == null) {
            usedAt = LocalDateTime.now();
        }
    }
}
