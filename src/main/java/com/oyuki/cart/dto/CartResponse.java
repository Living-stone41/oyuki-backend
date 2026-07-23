package com.oyuki.cart.dto;

import com.oyuki.cart.entity.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(

        Long cartId,
        Long customerId,
        String customerName,

        List<CartItemResponse> items,

        int totalItems,
        BigDecimal subtotal

) {

    public static CartResponse from(Cart cart) {

        List<CartItemResponse> itemResponses =
                cart.getItems()
                        .stream()
                        .map(CartItemResponse::from)
                        .toList();

        int totalItems = cart.getItems()
                .stream()
                .mapToInt(item -> item.getQuantity())
                .sum();

        BigDecimal subtotal = itemResponses
                .stream()
                .map(CartItemResponse::lineTotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        return new CartResponse(
                cart.getId(),
                cart.getCustomer().getId(),
                cart.getCustomer().getFullName(),
                itemResponses,
                totalItems,
                subtotal
        );
    }
}