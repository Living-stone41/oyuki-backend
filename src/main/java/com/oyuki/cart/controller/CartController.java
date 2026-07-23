package com.oyuki.cart.controller;

import com.oyuki.cart.dto.AddCartItemRequest;
import com.oyuki.cart.dto.CartResponse;
import com.oyuki.cart.dto.UpdateCartItemRequest;
import com.oyuki.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /*
     * VIEW CART
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            Authentication authentication
    ) {
        Long customerId = getUserId(authentication);

        return ResponseEntity.ok(
                cartService.getCart(customerId)
        );
    }

    /*
     * ADD ITEM
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        Long customerId = getUserId(authentication);

        CartResponse response = cartService.addItem(
                customerId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * UPDATE ITEM QUANTITY
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItem(
            Authentication authentication,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        Long customerId = getUserId(authentication);

        return ResponseEntity.ok(
                cartService.updateItem(
                        customerId,
                        cartItemId,
                        request
                )
        );
    }

    /*
     * REMOVE ONE ITEM
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            Authentication authentication,
            @PathVariable Long cartItemId
    ) {
        Long customerId = getUserId(authentication);

        return ResponseEntity.ok(
                cartService.removeItem(
                        customerId,
                        cartItemId
                )
        );
    }

    /*
     * CLEAR CART
     */
    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(
            Authentication authentication
    ) {
        Long customerId = getUserId(authentication);

        return ResponseEntity.ok(
                cartService.clearCart(customerId)
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}