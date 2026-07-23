package com.oyuki.providerlocation.entity;

import com.oyuki.delivery.enums.NigeriaState;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "provider_pickup_addresses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_provider_pickup_address",
                        columnNames = "provider_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_provider_pickup_state",
                        columnList = "state"
                ),
                @Index(
                        name = "idx_provider_pickup_city",
                        columnList = "city"
                ),
                @Index(
                        name = "idx_provider_pickup_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderPickupAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "provider_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_provider_pickup_provider"
            )
    )
    private User provider;

    @Column(
            name = "country",
            nullable = false,
            length = 80
    )
    @Builder.Default
    private String country = "Nigeria";

    @Enumerated(EnumType.STRING)
    @Column(
            name = "state",
            nullable = false,
            length = 50
    )
    private NigeriaState state;

    @Column(
            name = "city",
            nullable = false,
            length = 120
    )
    private String city;

    @Column(
            name = "lga",
            nullable = false,
            length = 120
    )
    private String lga;

    @Column(
            name = "area",
            nullable = false,
            length = 160
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
            name = "latitude",
            precision = 10,
            scale = 7
    )
    private BigDecimal latitude;

    @Column(
            name = "longitude",
            precision = 10,
            scale = 7
    )
    private BigDecimal longitude;

    @Column(
            name = "active",
            nullable = false
    )
    @Builder.Default
    private boolean active = true;

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

        if (country == null || country.isBlank()) {
            country = "Nigeria";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
