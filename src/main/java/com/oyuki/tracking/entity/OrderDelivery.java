package com.oyuki.tracking.entity;

import com.oyuki.order.entity.Order;
import com.oyuki.tracking.enums.OrderDeliveryStatus;
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
        name = "order_deliveries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_delivery_order",
                        columnNames = "order_id"
                ),
                @UniqueConstraint(
                        name = "uk_order_delivery_tracking_number",
                        columnNames = "tracking_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_order_delivery_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_order_delivery_rider",
                        columnList = "rider_id"
                ),
                @Index(
                        name = "idx_order_delivery_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_order_delivery_tracking",
                        columnList = "tracking_number"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * One combined customer order has one final
     * delivery from the Oyuki hub.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_order_delivery_order"
            )
    )
    private Order order;

    /*
     * Rider assigned to deliver the complete order.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "rider_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_delivery_rider"
            )
    )
    private User rider;

    /*
     * Admin who assigned the rider.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "assigned_by_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_delivery_assigned_by"
            )
    )
    private User assignedBy;

    @Column(
            name = "tracking_number",
            nullable = false,
            unique = true,
            length = 100
    )
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    @Builder.Default
    private OrderDeliveryStatus status =
            OrderDeliveryStatus.ASSIGNED;

    /*
     * Optional note entered by the admin
     * when assigning the rider.
     */
    @Column(
            name = "admin_note",
            length = 1000
    )
    private String adminNote;

    /*
     * Optional note entered by the rider.
     */
    @Column(
            name = "rider_note",
            length = 1000
    )
    private String riderNote;

    /*
     * Reason for a cancelled or failed delivery.
     */
    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    @Column(
            name = "assigned_at",
            nullable = false
    )
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "out_for_delivery_at")
    private LocalDateTime outForDeliveryAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

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

        if (assignedAt == null) {
            assignedAt = now;
        }

        if (status == null) {
            status =
                    OrderDeliveryStatus.ASSIGNED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt =
                LocalDateTime.now();
    }
}