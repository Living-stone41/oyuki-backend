package com.oyuki.address.entity;

import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "customer_addresses",
        indexes = {
                @Index(
                        name = "idx_address_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_address_default",
                        columnList = "customer_id,is_default"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_address_customer"
            )
    )
    private User customer;

    /*
     * Examples:
     * Home, Work, School, Parents' House.
     */
    @Column(
            name = "label",
            nullable = false,
            length = 50
    )
    private String label;

    @Column(
            name = "recipient_name",
            nullable = false,
            length = 150
    )
    private String recipientName;

    @Column(
            name = "phone",
            nullable = false,
            length = 30
    )
    private String phone;

    @Column(
            name = "state",
            nullable = false,
            length = 100
    )
    private String state;

    @Column(
            name = "city",
            nullable = false,
            length = 100
    )
    private String city;

    @Column(
            name = "area",
            nullable = false,
            length = 150
    )
    private String area;

    @Column(
            name = "street_address",
            nullable = false,
            length = 500
    )
    private String streetAddress;

    @Column(
            name = "landmark",
            length = 255
    )
    private String landmark;

    @Column(
            name = "postal_code",
            length = 30
    )
    private String postalCode;

    @Column(
            name = "delivery_instructions",
            length = 1000
    )
    private String deliveryInstructions;

    @Column(
            name = "is_default",
            nullable = false
    )
    @Builder.Default
    private boolean defaultAddress = false;

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