package com.oyuki.product.dto;

import com.oyuki.product.entity.Product;
import com.oyuki.product.entity.ProductImage;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.enums.ProductType;
import com.oyuki.user.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(

        Long id,
        Long ownerId,
        String ownerName,
        Role ownerRole,

        String name,
        String description,
        String category,

        ProductType productType,
        ProductStatus status,

        String state,
        String lga,
        String area,

        BigDecimal averageRating,
        int ratingCount,
        long viewCount,

        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductResponse from(Product product) {

        List<ProductVariantResponse> variants =
                product.getVariants()
                        .stream()
                        .map(ProductVariantResponse::from)
                        .toList();

        List<ProductImageResponse> images =
                product.getImages()
                        .stream()
                        .map(ProductImageResponse::from)
                        .toList();

        return new ProductResponse(
                product.getId(),
                product.getOwner().getId(),
                product.getOwner().getFullName(),
                product.getOwner().getRole(),

                product.getName(),
                product.getDescription(),
                product.getCategory(),

                product.getProductType(),
                product.getStatus(),

                product.getState(),
                product.getLga(),
                product.getArea(),

                product.getAverageRating(),
                product.getRatingCount(),
                product.getViewCount(),

                variants,
                images,

                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public record ProductImageResponse(
            Long id,
            String imageUrl,
            boolean primaryImage,
            int displayOrder
    ) {

        public static ProductImageResponse from(
                ProductImage image
        ) {
            return new ProductImageResponse(
                    image.getId(),
                    image.getImageUrl(),
                    image.isPrimaryImage(),
                    image.getDisplayOrder()
            );
        }
    }
}