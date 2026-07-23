package com.oyuki.order.dto;

import com.oyuki.order.entity.OrderItem;

import java.math.BigDecimal;

public record AdminOrderItemResponse(

        Long id,

        Long productId,
        String productName,

        Long providerId,
        String providerName,
        String providerEmail,
        String providerPhone,
        String providerRole,

        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,

        String status

) {

    public static AdminOrderItemResponse from(
            OrderItem orderItem
    ) {
        return new AdminOrderItemResponse(
                orderItem.getId(),

                orderItem.getProduct() == null
                        ? null
                        : orderItem.getProduct().getId(),

                orderItem.getProductName(),

                orderItem.getOwner() == null
                        ? null
                        : orderItem.getOwner().getId(),

                orderItem.getOwner() == null
                        ? "Unknown provider"
                        : orderItem.getOwner().getFullName(),

                orderItem.getOwner() == null
                        ? null
                        : orderItem.getOwner().getEmail(),

                orderItem.getOwner() == null
                        ? null
                        : orderItem.getOwner().getPhoneNumber(),

                orderItem.getOwner() == null
                        || orderItem.getOwner().getRole() == null
                        ? null
                        : orderItem.getOwner().getRole().name(),

                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getLineTotal() == null
                        ? BigDecimal.ZERO
                        : orderItem.getLineTotal(),

                orderItem.getStatus() == null
                        ? null
                        : orderItem.getStatus().name()
        );
    }
}
