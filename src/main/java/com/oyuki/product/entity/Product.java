package com.oyuki.product.entity;

import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.enums.ProductType;
import com.oyuki.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(
                        name = "idx_products_owner",
                        columnList = "owner_id"
                ),
                @Index(
                        name = "idx_products_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_products_type",
                        columnList = "product_type"
                ),
                @Index(
                        name = "idx_products_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * The owner can be either an approved SELLER
     * or an approved KITCHEN.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_products_owner"
            )
    )
    private User owner;

    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;

    @Column(
            name = "description",
            nullable = false,
            length = 2000
    )
    private String description;

    @Column(
            name = "category",
            nullable = false,
            length = 100
    )
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "product_type",
            nullable = false,
            length = 30
    )
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    /*
     * Location fields help the marketplace filter
     * products by state, LGA and area.
     */
    @Column(
            name = "state",
            nullable = false,
            length = 100
    )
    private String state;

    @Column(
            name = "lga",
            nullable = false,
            length = 100
    )
    private String lga;

    @Column(
            name = "area",
            nullable = false,
            length = 150
    )
    private String area;

    @Column(
            name = "average_rating",
            nullable = false,
            precision = 3,
            scale = 2
    )
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(
            name = "rating_count",
            nullable = false
    )
    @Builder.Default
    private int ratingCount = 0;

    @Column(
            name = "view_count",
            nullable = false
    )
    @Builder.Default
    private long viewCount = 0L;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ProductVariant> variants =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ProductImage> images =
            new ArrayList<>();

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
            status = ProductStatus.DRAFT;
        }

        if (averageRating == null) {
            averageRating = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }
}