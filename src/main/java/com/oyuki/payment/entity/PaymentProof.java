package com.oyuki.payment.entity;

import com.oyuki.order.entity.Order;
import com.oyuki.payment.enums.PaymentProofStatus;
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
        name = "payment_proofs",
        indexes = {
                @Index(
                        name = "idx_payment_proof_order",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_payment_proof_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_payment_proof_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_payment_proof_created",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * One order can have more than one receipt
     * submission if an earlier receipt was rejected.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_payment_proof_order"
            )
    )
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_payment_proof_customer"
            )
    )
    private User customer;

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    /*
     * Bank used by the customer to make
     * the transfer.
     */
    @Column(
            name = "sender_bank_name",
            nullable = false,
            length = 150
    )
    private String senderBankName;

    /*
     * Name appearing on the customer's
     * transfer receipt.
     */
    @Column(
            name = "sender_account_name",
            nullable = false,
            length = 200
    )
    private String senderAccountName;

    @Column(
            name = "transaction_reference",
            nullable = false,
            length = 200
    )
    private String transactionReference;

    /*
     * Date and time the customer says
     * the transfer was made.
     */
    @Column(
            name = "payment_date",
            nullable = false
    )
    private LocalDateTime paymentDate;

    /*
     * Relative path to uploaded JPG, PNG,
     * WEBP or PDF receipt.
     */
    @Column(
            name = "receipt_url",
            nullable = false,
            length = 1000
    )
    private String receiptUrl;

    @Column(
            name = "original_file_name",
            length = 500
    )
    private String originalFileName;

    @Column(
            name = "file_content_type",
            length = 150
    )
    private String fileContentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    @Builder.Default
    private PaymentProofStatus status =
            PaymentProofStatus.SUBMITTED;

    /*
     * Admin who confirmed or rejected
     * the uploaded receipt.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reviewed_by_id",
            foreignKey = @ForeignKey(
                    name = "fk_payment_proof_reviewed_by"
            )
    )
    private User reviewedBy;

    @Column(
            name = "admin_note",
            length = 1000
    )
    private String adminNote;

    @Column(
            name = "rejection_reason",
            length = 1000
    )
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

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
            status = PaymentProofStatus.SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}