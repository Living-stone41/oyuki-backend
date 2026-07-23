package com.oyuki.order.entity;

import com.oyuki.order.enums.OrderItemStatus;
import com.oyuki.product.entity.Product;
import com.oyuki.product.entity.ProductVariant;
import com.oyuki.product.enums.MeasurementUnit;
import com.oyuki.product.enums.ProductType;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(
                        name = "idx_order_items_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_order_items_owner",
                        columnList = "owner_id"
                ),
                @Index(
                        name = "idx_order_items_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_items_order"
            )
    )
    private Order order;

    /*
     * Seller or kitchen responsible for this item.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_items_owner"
            )
    )
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_items_product"
            )
    )
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "variant_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_items_variant"
            )
    )
    private ProductVariant variant;

    /*
     * Snapshot fields preserve the order details even if
     * the seller later changes the product or price.
     */
    @Column(
            name = "product_name",
            nullable = false,
            length = 150
    )
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "product_type",
            nullable = false,
            length = 30
    )
    private ProductType productType;

    @Column(
            name = "measurement_value",
            nullable = false,
            precision = 10,
            scale = 3
    )
    private BigDecimal measurementValue;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "measurement_unit",
            nullable = false,
            length = 30
    )
    private MeasurementUnit measurementUnit;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
            name = "quantity",
            nullable = false
    )
    private int quantity;

    @Column(
            name = "line_total",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal lineTotal;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private OrderItemStatus status = OrderItemStatus.PENDING;

    @Column(
            name = "rejection_reason",
            length = 1000
    )
    private String rejectionReason;

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

        if (status == null) {
            status = OrderItemStatus.PENDING;
        }

        if (
                lineTotal == null &&
                unitPrice != null &&
                quantity > 0
        ) {
            lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}