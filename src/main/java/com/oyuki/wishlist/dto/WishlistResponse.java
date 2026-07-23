package com.oyuki.wishlist.dto;

import com.oyuki.product.entity.Product;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.wishlist.entity.WishlistItem;

import java.time.LocalDateTime;

public record WishlistResponse(

        Long wishlistItemId,

        Long productId,
        String productName,
        ProductStatus productStatus,

        boolean availableForPurchase,

        LocalDateTime addedAt

) {

    public static WishlistResponse from(
            WishlistItem wishlistItem
    ) {
        Product product =
                wishlistItem.getProduct();

        boolean availableForPurchase =
                product != null &&
                product.getStatus()
                        == ProductStatus.ACTIVE;

        return new WishlistResponse(
                wishlistItem.getId(),

                product == null
                        ? null
                        : product.getId(),

                product == null
                        ? null
                        : product.getName(),

                product == null
                        ? null
                        : product.getStatus(),

                availableForPurchase,

                wishlistItem.getCreatedAt()
        );
    }
}