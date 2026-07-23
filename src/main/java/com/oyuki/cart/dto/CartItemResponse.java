package com.oyuki.cart.dto;

import com.oyuki.cart.entity.CartItem;
import com.oyuki.product.entity.Product;
import com.oyuki.product.entity.ProductImage;
import com.oyuki.product.entity.ProductVariant;
import com.oyuki.product.enums.MeasurementUnit;
import com.oyuki.product.enums.ProductType;
import com.oyuki.user.enums.Role;

import java.math.BigDecimal;

public record CartItemResponse(

        Long cartItemId,

        Long productId,
        String productName,
        String category,
        ProductType productType,

        Long ownerId,
        String ownerName,
        Role ownerRole,

        Long variantId,
        BigDecimal measurementValue,
        MeasurementUnit measurementUnit,
        BigDecimal unitPrice,

        int quantity,
        BigDecimal lineTotal,

        int availableStock,
        boolean available,

        String primaryImageUrl

) {

    public static CartItemResponse from(CartItem cartItem) {

        ProductVariant variant = cartItem.getVariant();
        Product product = variant.getProduct();

        BigDecimal lineTotal = variant.getPrice()
                .multiply(
                        BigDecimal.valueOf(
                                cartItem.getQuantity()
                        )
                );

        String primaryImageUrl = product.getImages()
                .stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() ->
                        product.getImages()
                                .stream()
                                .map(ProductImage::getImageUrl)
                                .findFirst()
                                .orElse(null)
                );

        return new CartItemResponse(
                cartItem.getId(),

                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getProductType(),

                product.getOwner().getId(),
                product.getOwner().getFullName(),
                product.getOwner().getRole(),

                variant.getId(),
                variant.getMeasurementValue(),
                variant.getMeasurementUnit(),
                variant.getPrice(),

                cartItem.getQuantity(),
                lineTotal,

                variant.getStockQuantity(),
                variant.isAvailable(),

                primaryImageUrl
        );
    }
}