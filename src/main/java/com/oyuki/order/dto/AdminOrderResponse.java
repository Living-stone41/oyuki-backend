package com.oyuki.order.dto;

import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(

        Long id,
        String orderNumber,

        Long customerId,
        String customerName,
        String customerEmail,
        String customerPhone,

        BigDecimal subtotal,
        BigDecimal deliveryFee,
        String couponCode,
        BigDecimal discountAmount,
        BigDecimal totalAmount,

        String paymentMethod,
        String paymentStatus,
        String orderStatus,

        String deliveryType,
        String recipientName,
        String recipientPhone,
        String state,
        String lga,
        String area,
        String addressLine,
        String deliveryInstructions,

        int totalItems,
        int totalProviders,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        List<AdminOrderItemResponse> items

) {

    public static AdminOrderResponse from(
            Order order,
            List<OrderItem> orderItems
    ) {
        List<AdminOrderItemResponse> itemResponses =
                orderItems.stream()
                        .map(AdminOrderItemResponse::from)
                        .toList();

        int providerCount =
                (int) orderItems.stream()
                        .map(OrderItem::getOwner)
                        .filter(owner -> owner != null)
                        .map(owner -> owner.getId())
                        .distinct()
                        .count();

        int totalQuantity =
                orderItems.stream()
                        .mapToInt(OrderItem::getQuantity)
                        .sum();

        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNumber(),

                order.getCustomer() == null
                        ? null
                        : order.getCustomer().getId(),

                order.getCustomer() == null
                        ? null
                        : order.getCustomer().getFullName(),

                order.getCustomer() == null
                        ? null
                        : order.getCustomer().getEmail(),

                order.getCustomer() == null
                        ? null
                        : order.getCustomer().getPhoneNumber(),

                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getCouponCode(),
                order.getDiscountAmount(),
                order.getTotalAmount(),

                order.getPaymentMethod() == null
                        ? null
                        : order.getPaymentMethod().name(),

                order.getPaymentStatus() == null
                        ? null
                        : order.getPaymentStatus().name(),

                order.getStatus() == null
                        ? null
                        : order.getStatus().name(),

                order.getDeliveryType() == null
                        ? null
                        : order.getDeliveryType().name(),

                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getState(),
                order.getLga(),
                order.getArea(),
                order.getAddressLine(),
                order.getDeliveryInstructions(),

                totalQuantity,
                providerCount,

                order.getCreatedAt(),
                order.getUpdatedAt(),

                itemResponses
        );
    }
}
