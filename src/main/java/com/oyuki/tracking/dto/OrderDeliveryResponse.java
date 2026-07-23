package com.oyuki.tracking.dto;

import com.oyuki.tracking.entity.OrderDelivery;
import com.oyuki.tracking.enums.OrderDeliveryStatus;

import java.time.LocalDateTime;

public record OrderDeliveryResponse(

        Long id,

        Long orderId,
        String orderNumber,

        String trackingNumber,
        OrderDeliveryStatus status,

        Long customerId,
        String customerName,
        String customerPhone,

        String recipientName,
        String recipientPhone,

        String state,
        String lga,
        String area,
        String addressLine,
        String deliveryInstructions,

        Long riderId,
        String riderName,
        String riderEmail,
        String riderPhone,

        Long assignedById,
        String assignedByName,

        String adminNote,
        String riderNote,
        String failureReason,

        LocalDateTime assignedAt,
        LocalDateTime acceptedAt,
        LocalDateTime pickedUpAt,
        LocalDateTime outForDeliveryAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static OrderDeliveryResponse from(
            OrderDelivery delivery
    ) {
        return new OrderDeliveryResponse(
                delivery.getId(),

                delivery.getOrder().getId(),
                delivery.getOrder().getOrderNumber(),

                delivery.getTrackingNumber(),
                delivery.getStatus(),

                delivery.getOrder().getCustomer() == null
                        ? null
                        : delivery.getOrder()
                                .getCustomer()
                                .getId(),

                delivery.getOrder().getCustomer() == null
                        ? null
                        : delivery.getOrder()
                                .getCustomer()
                                .getFullName(),

                delivery.getOrder().getCustomer() == null
                        ? null
                        : delivery.getOrder()
                                .getCustomer()
                                .getPhoneNumber(),

                delivery.getOrder().getRecipientName(),
                delivery.getOrder().getRecipientPhone(),

                delivery.getOrder().getState(),
                delivery.getOrder().getLga(),
                delivery.getOrder().getArea(),
                delivery.getOrder().getAddressLine(),
                delivery.getOrder()
                        .getDeliveryInstructions(),

                delivery.getRider().getId(),
                delivery.getRider().getFullName(),
                delivery.getRider().getEmail(),
                delivery.getRider().getPhoneNumber(),

                delivery.getAssignedBy().getId(),
                delivery.getAssignedBy().getFullName(),

                delivery.getAdminNote(),
                delivery.getRiderNote(),
                delivery.getFailureReason(),

                delivery.getAssignedAt(),
                delivery.getAcceptedAt(),
                delivery.getPickedUpAt(),
                delivery.getOutForDeliveryAt(),
                delivery.getDeliveredAt(),
                delivery.getCancelledAt(),

                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}