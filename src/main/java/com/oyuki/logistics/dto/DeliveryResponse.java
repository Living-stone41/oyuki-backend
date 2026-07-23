package com.oyuki.logistics.dto;

import com.oyuki.logistics.entity.Delivery;
import com.oyuki.logistics.enums.DeliveryStatus;
import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryResponse(

        Long id,
        String trackingNumber,
        DeliveryStatus status,

        Long orderId,
        String orderNumber,

        Long orderItemId,
        String productName,
        int quantity,

        Long providerId,
        String providerName,
        String providerRole,

        Long customerId,
        String customerName,
        String customerPhone,

        Long riderId,
        String riderName,
        String riderPhone,

        Long assignedById,
        String assignedByName,

        String recipientName,
        String recipientPhone,

        String state,
        String lga,
        String area,
        String addressLine,
        String deliveryInstructions,

        BigDecimal deliveryFee,

        BigDecimal currentLatitude,
        BigDecimal currentLongitude,
        LocalDateTime lastLocationUpdateAt,

        String deliveryNote,
        String failureReason,

        LocalDateTime assignedAt,
        LocalDateTime acceptedAt,
        LocalDateTime pickedUpAt,
        LocalDateTime outForDeliveryAt,
        LocalDateTime deliveredAt,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static DeliveryResponse from(
            Delivery delivery
    ) {
        Order order = delivery.getOrder();
        OrderItem orderItem = delivery.getOrderItem();

        User provider = orderItem.getOwner();
        User customer = order.getCustomer();
        User rider = delivery.getRider();
        User assignedBy = delivery.getAssignedBy();

        return new DeliveryResponse(
                delivery.getId(),
                delivery.getTrackingNumber(),
                delivery.getStatus(),

                order.getId(),
                order.getOrderNumber(),

                orderItem.getId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),

                provider.getId(),
                provider.getFullName(),
                provider.getRole().name(),

                customer.getId(),
                customer.getFullName(),
                customer.getPhoneNumber(),

                rider == null ? null : rider.getId(),
                rider == null ? null : rider.getFullName(),
                rider == null ? null : rider.getPhoneNumber(),

                assignedBy == null
                        ? null
                        : assignedBy.getId(),

                assignedBy == null
                        ? null
                        : assignedBy.getFullName(),

                order.getRecipientName(),
                order.getRecipientPhone(),

                order.getState(),
                order.getLga(),
                order.getArea(),
                order.getAddressLine(),
                order.getDeliveryInstructions(),

                delivery.getDeliveryFee(),

                delivery.getCurrentLatitude(),
                delivery.getCurrentLongitude(),
                delivery.getLastLocationUpdateAt(),

                delivery.getDeliveryNote(),
                delivery.getFailureReason(),

                delivery.getAssignedAt(),
                delivery.getAcceptedAt(),
                delivery.getPickedUpAt(),
                delivery.getOutForDeliveryAt(),
                delivery.getDeliveredAt(),

                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}