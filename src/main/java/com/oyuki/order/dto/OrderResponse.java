package com.oyuki.order.dto;

import com.oyuki.order.entity.Order;
import com.oyuki.order.enums.DeliveryType;
import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.enums.PaymentMethod;
import com.oyuki.order.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id, String orderNumber,
        Long customerId, String customerName, String customerEmail,
        OrderStatus status,
        DeliveryType deliveryType, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
        Long destinationKitchenId, String destinationKitchenName,
        String recipientName, String recipientPhone,
        String state, String lga, String area, String addressLine, String deliveryInstructions,
        BigDecimal subtotal, BigDecimal deliveryFee,
        String couponCode, BigDecimal discountAmount,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() == null ? List.of() : order.getItems().stream().map(OrderItemResponse::from).toList();
        Long kitchenId = order.getDestinationKitchen() == null ? null : order.getDestinationKitchen().getId();
        String kitchenName = order.getDestinationKitchen() == null ? null : order.getDestinationKitchen().getFullName();
        return new OrderResponse(
                order.getId(), order.getOrderNumber(),
                order.getCustomer().getId(), order.getCustomer().getFullName(), order.getCustomer().getEmail(),
                order.getStatus(), order.getDeliveryType(), order.getPaymentMethod(), order.getPaymentStatus(),
                kitchenId, kitchenName,
                order.getRecipientName(), order.getRecipientPhone(),
                order.getState(), order.getLga(), order.getArea(), order.getAddressLine(), order.getDeliveryInstructions(),
                order.getSubtotal() == null ? BigDecimal.ZERO : order.getSubtotal(),
                order.getDeliveryFee() == null ? BigDecimal.ZERO : order.getDeliveryFee(),
                order.getCouponCode(),
                order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount(),
                order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount(),
                itemResponses, order.getCreatedAt(), order.getUpdatedAt()
        );
    }
}
