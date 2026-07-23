package com.oyuki.delivery.entity;

import com.oyuki.delivery.enums.DeliveryRateType;
import com.oyuki.delivery.enums.NigeriaState;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "delivery_rates",
        indexes = {
                @Index(
                        name = "idx_delivery_rate_type",
                        columnList = "rate_type"
                ),
                @Index(
                        name = "idx_delivery_rate_origin_state",
                        columnList = "origin_state"
                ),
                @Index(
                        name = "idx_delivery_rate_destination_state",
                        columnList = "destination_state"
                ),
                @Index(
                        name = "idx_delivery_rate_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "rate_type",
            nullable = false,
            length = 40
    )
    private DeliveryRateType rateType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "origin_state",
            length = 50
    )
    private NigeriaState originState;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "destination_state",
            length = 50
    )
    private NigeriaState destinationState;

    @Column(
            name = "origin_city",
            length = 120
    )
    private String originCity;

    @Column(
            name = "destination_city",
            length = 120
    )
    private String destinationCity;

    @Column(
            name = "origin_area",
            length = 160
    )
    private String originArea;

    @Column(
            name = "destination_area",
            length = 160
    )
    private String destinationArea;

    @Column(
            name = "fee",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal fee;

    @Column(
            name = "estimated_min_days",
            nullable = false
    )
    @Builder.Default
    private Integer estimatedMinDays = 0;

    @Column(
            name = "estimated_max_days",
            nullable = false
    )
    @Builder.Default
    private Integer estimatedMaxDays = 1;

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
                    name = "fk_delivery_rate_created_by"
            )
    )
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by_id",
            foreignKey = @ForeignKey(
                    name = "fk_delivery_rate_updated_by"
            )
    )
    private User updatedBy;

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

        if (estimatedMinDays == null) {
            estimatedMinDays = 0;
        }

        if (estimatedMaxDays == null) {
            estimatedMaxDays = Math.max(1, estimatedMinDays);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
