package com.oyuki.logistics.entity;

import com.oyuki.logistics.enums.DeliveryStatus;
import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "deliveries",
        indexes = {
                @Index(
                        name = "idx_deliveries_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_deliveries_order_item",
                        columnList = "order_item_id"
                ),
                @Index(
                        name = "idx_deliveries_rider",
                        columnList = "rider_id"
                ),
                @Index(
                        name = "idx_deliveries_status",
                        columnList = "status"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_deliveries_tracking_number",
                        columnNames = "tracking_number"
                ),
                @UniqueConstraint(
                        name = "uk_deliveries_order_item",
                        columnNames = "order_item_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Public number customers can use
     * to track the delivery.
     */
    @Column(
            name = "tracking_number",
            nullable = false,
            unique = true,
            length = 50
    )
    private String trackingNumber;

    /*
     * The main customer order.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_deliveries_order"
            )
    )
    private Order order;

    /*
     * Each delivery belongs to one order item.
     *
     * This is important because one customer order
     * may contain products from different sellers
     * or kitchens.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_item_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_deliveries_order_item"
            )
    )
    private OrderItem orderItem;

    /*
     * The rider assigned to this delivery.
     *
     * It remains null until the logistics admin
     * assigns a rider.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "rider_id",
            foreignKey = @ForeignKey(
                    name = "fk_deliveries_rider"
            )
    )
    private User rider;

    /*
     * The logistics administrator who assigned
     * the rider.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assigned_by_id",
            foreignKey = @ForeignKey(
                    name = "fk_deliveries_assigned_by"
            )
    )
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    @Builder.Default
    private DeliveryStatus status =
            DeliveryStatus.PENDING_ASSIGNMENT;

    /*
     * Delivery charge for this particular item.
     */
    @Column(
            name = "delivery_fee",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    /*
     * Rider's latest location.
     * These fields will be used during live tracking.
     */
    @Column(
            name = "current_latitude",
            precision = 10,
            scale = 7
    )
    private BigDecimal currentLatitude;

    @Column(
            name = "current_longitude",
            precision = 10,
            scale = 7
    )
    private BigDecimal currentLongitude;

    @Column(name = "last_location_update_at")
    private LocalDateTime lastLocationUpdateAt;

    /*
     * Optional information entered by
     * the logistics administrator or rider.
     */
    @Column(
            name = "delivery_note",
            length = 1000
    )
    private String deliveryNote;

    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "out_for_delivery_at")
    private LocalDateTime outForDeliveryAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

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

        if (
                trackingNumber == null ||
                trackingNumber.isBlank()
        ) {
            trackingNumber =
                    "OYU-DEL-" +
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 10)
                            .toUpperCase();
        }

        if (status == null) {
            status =
                    DeliveryStatus.PENDING_ASSIGNMENT;
        }

        if (deliveryFee == null) {
            deliveryFee = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}