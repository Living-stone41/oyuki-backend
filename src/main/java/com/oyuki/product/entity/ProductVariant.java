package com.oyuki.product.entity;

import com.oyuki.product.enums.MeasurementUnit;
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
        name = "product_variants",
        indexes = {
                @Index(
                        name = "idx_product_variants_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_product_variants_unit",
                        columnList = "measurement_unit"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_variants_product"
            )
    )
    private Product product;

    /*
     * Examples:
     * measurementValue = 1
     * measurementUnit = KILOGRAM
     *
     * measurementValue = 5
     * measurementUnit = CUP
     */
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
            name = "price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal price;

    /*
     * Number of this variant currently available.
     *
     * Example:
     * 40 packs of the 1 KG option.
     */
    @Column(
            name = "stock_quantity",
            nullable = false
    )
    @Builder.Default
    private int stockQuantity = 0;

    @Column(
            name = "minimum_order_quantity",
            nullable = false
    )
    @Builder.Default
    private int minimumOrderQuantity = 1;

    @Column(
            name = "sku",
            unique = true,
            length = 100
    )
    private String sku;

    @Column(
            name = "available",
            nullable = false
    )
    @Builder.Default
    private boolean available = true;

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

        if (minimumOrderQuantity < 1) {
            minimumOrderQuantity = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}