package com.oyuki.order.entity;

import com.oyuki.order.enums.DeliveryType;
import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.enums.PaymentMethod;
import com.oyuki.order.enums.PaymentStatus;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(
                        name = "idx_orders_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_orders_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_orders_payment_status",
                        columnList = "payment_status"
                ),
                @Index(
                        name = "idx_orders_coupon_code",
                        columnList = "coupon_code"
                ),
                @Index(
                        name = "idx_orders_created_at",
                        columnList = "created_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_orders_order_number",
                        columnNames = "order_number"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "order_number",
            nullable = false,
            unique = true,
            length = 50
    )
    private String orderNumber;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_orders_customer"
            )
    )
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    @Builder.Default
    private OrderStatus status =
            OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "delivery_type",
            nullable = false,
            length = 50
    )
    private DeliveryType deliveryType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 50
    )
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_status",
            nullable = false,
            length = 50
    )
    @Builder.Default
    private PaymentStatus paymentStatus =
            PaymentStatus.PENDING;

    /*
     * Used when farm ingredients should be delivered
     * to a particular kitchen.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "destination_kitchen_id",
            foreignKey = @ForeignKey(
                    name = "fk_orders_destination_kitchen"
            )
    )
    private User destinationKitchen;

    /*
     * Delivery address snapshot.
     *
     * These values are copied into the order during
     * checkout so changing the customer's saved address
     * later will not change an existing order.
     */
    @Column(
            name = "recipient_name",
            nullable = false,
            length = 150
    )
    private String recipientName;

    @Column(
            name = "recipient_phone",
            nullable = false,
            length = 30
    )
    private String recipientPhone;

    @Column(
            name = "state",
            nullable = false,
            length = 100
    )
    private String state;

    @Column(
            name = "lga",
            nullable = false,
            length = 100
    )
    private String lga;

    @Column(
            name = "area",
            length = 150
    )
    private String area;

    @Column(
            name = "address_line",
            nullable = false,
            length = 500
    )
    private String addressLine;

    @Column(
            name = "delivery_instructions",
            length = 1000
    )
    private String deliveryInstructions;

    /*
     * Order subtotal before delivery fee and discount.
     */
    @Column(
            name = "subtotal",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal subtotal =
            BigDecimal.ZERO;

    /*
     * Automatically calculated nationwide delivery fee.
     */
    @Column(
            name = "delivery_fee",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal deliveryFee =
            BigDecimal.ZERO;

    /*
     * Permanent coupon snapshot.
     *
     * No relationship is created with Coupon because
     * the order should retain the applied coupon code
     * even if the coupon is changed later.
     */
    @Column(
            name = "coupon_code",
            length = 40
    )
    private String couponCode;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal discountAmount =
            BigDecimal.ZERO;

    /*
     * subtotal + deliveryFee - discountAmount
     */
    @Column(
            name = "total_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal totalAmount =
            BigDecimal.ZERO;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<OrderItem> items =
            new ArrayList<>();

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

        if (
                orderNumber == null ||
                orderNumber.isBlank()
        ) {
            orderNumber =
                    "OYU-" +
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 10)
                            .toUpperCase();
        }

        if (status == null) {
            status = OrderStatus.PENDING;
        }

        if (paymentStatus == null) {
            paymentStatus =
                    PaymentStatus.PENDING;
        }

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (deliveryFee == null) {
            deliveryFee = BigDecimal.ZERO;
        }

        if (discountAmount == null) {
            discountAmount =
                    BigDecimal.ZERO;
        }

        if (totalAmount == null) {
            totalAmount =
                    BigDecimal.ZERO;
        }

        if (
                couponCode != null &&
                couponCode.isBlank()
        ) {
            couponCode = null;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (deliveryFee == null) {
            deliveryFee = BigDecimal.ZERO;
        }

        if (discountAmount == null) {
            discountAmount =
                    BigDecimal.ZERO;
        }

        if (totalAmount == null) {
            totalAmount =
                    BigDecimal.ZERO;
        }

        if (
                couponCode != null &&
                couponCode.isBlank()
        ) {
            couponCode = null;
        }
    }

    public void addItem(
            OrderItem item
    ) {
        if (item == null) {
            return;
        }

        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(
            OrderItem item
    ) {
        if (item == null) {
            return;
        }

        items.remove(item);
        item.setOrder(null);
    }
}