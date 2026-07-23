package com.oyuki.payment.entity;

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
        name = "platform_bank_accounts",
        indexes = {
                @Index(
                        name = "idx_platform_bank_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "bank_name",
            nullable = false,
            length = 150
    )
    private String bankName;

    @Column(
            name = "account_name",
            nullable = false,
            length = 200
    )
    private String accountName;

    @Column(
            name = "account_number",
            nullable = false,
            length = 30
    )
    private String accountNumber;

    /*
     * Optional transfer instructions displayed
     * to the customer during checkout.
     */
    @Column(
            name = "payment_instructions",
            length = 2000
    )
    private String paymentInstructions;

    /*
     * Only the active bank account should be
     * displayed to customers.
     */
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
                    name = "fk_platform_bank_created_by"
            )
    )
    private User createdBy;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}