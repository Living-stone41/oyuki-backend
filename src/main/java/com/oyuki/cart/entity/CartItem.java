package com.oyuki.cart.entity;

import com.oyuki.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_variant",
                        columnNames = {
                                "cart_id",
                                "variant_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_cart_items_cart",
                        columnList = "cart_id"
                ),
                @Index(
                        name = "idx_cart_items_variant",
                        columnList = "variant_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cart_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_cart_items_cart"
            )
    )
    private Cart cart;

    /*
     * The selected measurement and price option.
     *
     * Examples:
     * 1 CUP of Garri
     * 1 KILOGRAM of Rice
     * 1 PLATE of Jollof Rice
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "variant_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_cart_items_variant"
            )
    )
    private ProductVariant variant;

    @Column(
            name = "quantity",
            nullable = false
    )
    @Builder.Default
    private int quantity = 1;

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

        if (quantity < 1) {
            quantity = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (quantity < 1) {
            quantity = 1;
        }
    }
}