package com.oyuki.wishlist.controller;

import com.oyuki.wishlist.dto.WishlistCheckResponse;
import com.oyuki.wishlist.dto.WishlistResponse;
import com.oyuki.wishlist.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(
            WishlistService wishlistService
    ) {
        this.wishlistService =
                wishlistService;
    }

    /*
     * Add product to wishlist.
     */
    @PostMapping("/products/{productId}")
    public ResponseEntity<WishlistResponse>
    addProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        WishlistResponse response =
                wishlistService.addProduct(
                        getUserId(authentication),
                        productId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get all saved products.
     */
    @GetMapping
    public ResponseEntity<List<WishlistResponse>>
    getWishlist(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                wishlistService.getWishlist(
                        getUserId(authentication)
                )
        );
    }

    /*
     * Check whether a product is saved.
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<WishlistCheckResponse>
    checkProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                wishlistService.checkProduct(
                        getUserId(authentication),
                        productId
                )
        );
    }

    /*
     * Return the number of saved products.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>>
    countWishlistItems(
            Authentication authentication
    ) {
        long count =
                wishlistService.countWishlistItems(
                        getUserId(authentication)
                );

        return ResponseEntity.ok(
                Map.of(
                        "count",
                        count
                )
        );
    }

    /*
     * Remove one saved product.
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void>
    removeProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        wishlistService.removeProduct(
                getUserId(authentication),
                productId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    /*
     * Remove every product from wishlist.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>>
    clearWishlist(
            Authentication authentication
    ) {
        long removedItems =
                wishlistService.clearWishlist(
                        getUserId(authentication)
                );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Wishlist cleared successfully",

                        "removedItems",
                        removedItems
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}