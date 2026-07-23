package com.oyuki.refund.entity;

import com.oyuki.order.entity.Order;
import com.oyuki.refund.enums.RefundStatus;
import com.oyuki.refund.enums.RefundType;
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
        name = "order_refunds",
        indexes = {
                @Index(
                        name = "idx_refund_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_refund_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_refund_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_refund_created",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Order connected to the refund.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_refund_order"
            )
    )
    private Order order;

    /*
     * Customer receiving the refund.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_refund_customer"
            )
    )
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "refund_type",
            nullable = false,
            length = 30
    )
    private RefundType refundType;

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "reason",
            nullable = false,
            length = 1000
    )
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    @Builder.Default
    private RefundStatus status =
            RefundStatus.PENDING;

    /*
     * Bank reference entered after the admin
     * transfers the refund to the customer.
     */
    @Column(
            name = "transaction_reference",
            length = 200
    )
    private String transactionReference;

    @Column(
            name = "admin_note",
            length = 1000
    )
    private String adminNote;

    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    /*
     * Admin who created or processed the refund.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_refund_created_by"
            )
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "processed_by_id",
            foreignKey = @ForeignKey(
                    name = "fk_refund_processed_by"
            )
    )
    private User processedBy;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

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

        if (status == null) {
            status = RefundStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}