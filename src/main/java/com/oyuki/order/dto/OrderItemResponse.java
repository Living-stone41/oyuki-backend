package com.oyuki.order.dto;

import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.OrderItemStatus;
import com.oyuki.product.enums.MeasurementUnit;
import com.oyuki.product.enums.ProductType;
import com.oyuki.user.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemResponse(

        Long id,

        Long orderId,
        String orderNumber,
        Long customerId,
        String customerName,

        Long ownerId,
        String ownerName,
        Role ownerRole,

        Long productId,
        Long variantId,

        String productName,
        ProductType productType,

        BigDecimal measurementValue,
        MeasurementUnit measurementUnit,

        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,

        OrderItemStatus status,
        String rejectionReason,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static OrderItemResponse from(
            OrderItem item
    ) {
        Order order = item.getOrder();

        return new OrderItemResponse(
                item.getId(),

                order == null
                        ? null
                        : order.getId(),

                order == null
                        ? null
                        : order.getOrderNumber(),

                order == null
                        || order.getCustomer() == null
                        ? null
                        : order.getCustomer().getId(),

                order == null
                        || order.getCustomer() == null
                        ? null
                        : order.getCustomer().getFullName(),

                item.getOwner().getId(),
                item.getOwner().getFullName(),
                item.getOwner().getRole(),

                item.getProduct().getId(),
                item.getVariant().getId(),

                item.getProductName(),
                item.getProductType(),

                item.getMeasurementValue(),
                item.getMeasurementUnit(),

                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal(),

                item.getStatus(),
                item.getRejectionReason(),

                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
