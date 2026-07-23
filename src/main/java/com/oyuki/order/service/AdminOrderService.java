package com.oyuki.order.service;

import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.order.dto.AdminOrderResponse;
import com.oyuki.order.dto.CancelOrderRequest;
import com.oyuki.order.dto.CancelOrderResponse;
import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.OrderItemStatus;
import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.enums.PaymentStatus;
import com.oyuki.order.repository.OrderItemRepository;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.product.entity.Product;
import com.oyuki.product.entity.ProductVariant;
import com.oyuki.product.enums.ProductStatus;
import com.oyuki.product.repository.ProductRepository;
import com.oyuki.product.repository.ProductVariantRepository;
import com.oyuki.refund.dto.RefundResponse;
import com.oyuki.refund.service.OrderRefundService;
import com.oyuki.tracking.entity.OrderDelivery;
import com.oyuki.tracking.enums.OrderDeliveryStatus;
import com.oyuki.tracking.repository.OrderDeliveryRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    private final OrderRefundService orderRefundService;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final NotificationService notificationService;

    public AdminOrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            OrderRefundService orderRefundService,
            OrderDeliveryRepository orderDeliveryRepository,
            NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderRefundService = orderRefundService;
        this.orderDeliveryRepository = orderDeliveryRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<AdminOrderResponse> getAllOrders(
            Long adminId
    ) {
        getActiveAdmin(adminId);

        return orderRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse getOrderById(
            Long adminId,
            Long orderId
    ) {
        getActiveAdmin(adminId);

        Order order =
                getOrder(orderId);

        return convertToResponse(order);
    }

    @Transactional
    public AdminOrderResponse markOrderItemReceived(
            Long adminId,
            Long orderItemId
    ) {
        getActiveAdmin(adminId);

        OrderItem orderItem =
                orderItemRepository
                        .findById(orderItemId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order item not found"
                                )
                        );

        Order order = orderItem.getOrder();

        if (order == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This item is not connected to an order"
            );
        }

        if (
                order.getPaymentStatus()
                        != PaymentStatus.PAID
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment must be confirmed before receiving goods"
            );
        }

        if (
                orderItem.getStatus()
                        == OrderItemStatus.RECEIVED_BY_ADMIN
        ) {
            return convertToResponse(order);
        }

        if (
                orderItem.getStatus()
                        != OrderItemStatus.READY_FOR_PICKUP
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only items marked READY_FOR_PICKUP can be received"
            );
        }

        orderItem.setStatus(
                OrderItemStatus.RECEIVED_BY_ADMIN
        );

        orderItemRepository.save(orderItem);

        updateOrderReadyStatus(order);

        return convertToResponse(order);
    }

    /*
     * =========================================================
     * ADMIN CANCELS ORDER
     * =========================================================
     */

    @Transactional
    public CancelOrderResponse cancelOrder(
            Long adminId,
            Long orderId,
            CancelOrderRequest request
    ) {
        getActiveAdmin(adminId);

        validateCancellationRequest(request);

        Order order = getOrder(orderId);

        if (
                order.getStatus()
                        == OrderStatus.CANCELLED
        ) {
            return new CancelOrderResponse(
                    "Order is already cancelled",
                    false,
                    convertToResponse(order),
                    null
            );
        }

        if (
                order.getStatus()
                        == OrderStatus.DELIVERED ||
                order.getStatus()
                        == OrderStatus.OUT_FOR_DELIVERY
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delivered or out-for-delivery orders cannot be cancelled normally"
            );
        }

        List<OrderItem> orderItems =
                orderItemRepository
                        .findAllByOrder_IdOrderByCreatedAtAsc(
                                orderId
                        );

        boolean hasDeliveredItem =
                orderItems.stream()
                        .anyMatch(item ->
                                item.getStatus()
                                        == OrderItemStatus.DELIVERED
                        );

        if (hasDeliveredItem) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "An order containing delivered items cannot be cancelled normally"
            );
        }

        cancelExistingDelivery(
                orderId,
                request.reason()
        );

        for (OrderItem item : orderItems) {
            if (
                    item.getStatus()
                            == OrderItemStatus.REJECTED ||
                    item.getStatus()
                            == OrderItemStatus.CANCELLED
            ) {
                continue;
            }

            restoreVariantStock(item);

            item.setStatus(
                    OrderItemStatus.CANCELLED
            );
        }

        orderItemRepository.saveAll(orderItems);

        order.setStatus(
                OrderStatus.CANCELLED
        );

        orderRepository.save(order);

        RefundResponse refund = null;

        boolean orderWasPaid =
                order.getPaymentStatus()
                        == PaymentStatus.PAID ||
                order.getPaymentStatus()
                        == PaymentStatus.PARTIALLY_REFUNDED;

        boolean createRefund =
                request.createRefund() == null ||
                request.createRefund();

        if (orderWasPaid && createRefund) {
            refund =
                    orderRefundService
                            .createFullRefundForCancellation(
                                    adminId,
                                    orderId,
                                    request.reason().trim(),
                                    clean(request.note())
                            );
        }

        notifyCustomerOrderCancelled(
                order,
                request.reason(),
                refund != null
        );

        return new CancelOrderResponse(
                refund == null
                        ? "Order cancelled successfully"
                        : "Order cancelled and full refund created",
                refund != null,
                convertToResponse(order),
                refund
        );
    }

    private void cancelExistingDelivery(
            Long orderId,
            String reason
    ) {
        Optional<OrderDelivery> optionalDelivery =
                orderDeliveryRepository
                        .findByOrder_Id(orderId);

        if (optionalDelivery.isEmpty()) {
            return;
        }

        OrderDelivery delivery =
                optionalDelivery.get();

        if (
                delivery.getStatus()
                        == OrderDeliveryStatus.DELIVERED ||
                delivery.getStatus()
                        == OrderDeliveryStatus.OUT_FOR_DELIVERY ||
                delivery.getStatus()
                        == OrderDeliveryStatus.PICKED_UP
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The rider has already started this delivery"
            );
        }

        delivery.setStatus(
                OrderDeliveryStatus.CANCELLED
        );

        delivery.setFailureReason(
                reason.trim()
        );

        delivery.setCancelledAt(
                LocalDateTime.now()
        );

        orderDeliveryRepository.save(delivery);
    }

    private void restoreVariantStock(
            OrderItem orderItem
    ) {
        ProductVariant variant =
                orderItem.getVariant();

        if (variant == null) {
            return;
        }

        int currentStock =
                variant.getStockQuantity();

        variant.setStockQuantity(
                currentStock +
                orderItem.getQuantity()
        );

        variant.setAvailable(true);

        productVariantRepository.save(variant);

        Product product =
                variant.getProduct();

        if (
                product != null &&
                product.getStatus()
                        == ProductStatus.OUT_OF_STOCK
        ) {
            product.setStatus(
                    ProductStatus.ACTIVE
            );

            productRepository.save(product);
        }
    }

    private void notifyCustomerOrderCancelled(
            Order order,
            String reason,
            boolean refundCreated
    ) {
        String message =
                "Order "
                        + order.getOrderNumber()
                        + " was cancelled. Reason: "
                        + reason.trim();

        if (refundCreated) {
            message +=
                    " A full refund has been created and is awaiting processing.";
        }

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.ORDER_CANCELLED,
                "Order cancelled",
                message,
                "ORDER",
                order.getId(),
                "/customer/orders/" + order.getId(),
                null
        );
    }

    private void updateOrderReadyStatus(
            Order order
    ) {
        List<OrderItem> items =
                orderItemRepository
                        .findAllByOrder_IdOrderByCreatedAtAsc(
                                order.getId()
                        );

        List<OrderItem> activeItems =
                items.stream()
                        .filter(item ->
                                item.getStatus()
                                        != OrderItemStatus.REJECTED
                        )
                        .filter(item ->
                                item.getStatus()
                                        != OrderItemStatus.CANCELLED
                        )
                        .toList();

        boolean allReceived =
                !activeItems.isEmpty() &&
                activeItems.stream()
                        .allMatch(item ->
                                item.getStatus()
                                        == OrderItemStatus.RECEIVED_BY_ADMIN ||
                                item.getStatus()
                                        == OrderItemStatus.DELIVERED
                        );

        if (allReceived) {
            order.setStatus(
                    OrderStatus.READY_FOR_DELIVERY
            );

            orderRepository.save(order);
        }
    }

    private AdminOrderResponse convertToResponse(
            Order order
    ) {
        List<OrderItem> items =
                orderItemRepository
                        .findAllByOrder_IdOrderByCreatedAtAsc(
                                order.getId()
                        );

        return AdminOrderResponse.from(
                order,
                items
        );
    }

    private Order getOrder(
            Long orderId
    ) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );
    }

    private void validateCancellationRequest(
            CancelOrderRequest request
    ) {
        if (
                request == null ||
                request.reason() == null ||
                request.reason().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cancellation reason is required"
            );
        }
    }

    private User getActiveAdmin(
            Long adminId
    ) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Administrator account not found"
                                )
                        );

        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can manage orders"
            );
        }

        if (
                admin.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator account is not active"
            );
        }

        return admin;
    }

    private String clean(
            String value
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }
}