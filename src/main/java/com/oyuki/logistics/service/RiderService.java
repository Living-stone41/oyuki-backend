package com.oyuki.logistics.service;

import com.oyuki.logistics.dto.DeliveryResponse;
import com.oyuki.logistics.dto.DeliveryStatusRequest;
import com.oyuki.logistics.entity.Delivery;
import com.oyuki.logistics.enums.DeliveryStatus;
import com.oyuki.logistics.repository.DeliveryRepository;
import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.OrderItemStatus;
import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.repository.OrderItemRepository;
import com.oyuki.order.repository.OrderRepository;
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

@Service
public class RiderService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RiderService(
            DeliveryRepository deliveryRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /*
     * RIDER VIEWS ALL ASSIGNED DELIVERIES.
     *
     * Examples:
     * GET /api/rider/deliveries
     * GET /api/rider/deliveries?status=ASSIGNED
     */
    @Transactional(readOnly = true)
    public List<DeliveryResponse> getMyDeliveries(
            Long riderId,
            DeliveryStatus status
    ) {
        getActiveRider(riderId);

        List<Delivery> deliveries;

        if (status == null) {
            deliveries =
                    deliveryRepository
                            .findAllByRider_IdOrderByCreatedAtDesc(
                                    riderId
                            );
        } else {
            deliveries =
                    deliveryRepository
                            .findAllByRider_IdAndStatusOrderByCreatedAtDesc(
                                    riderId,
                                    status
                            );
        }

        return deliveries.stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    /*
     * RIDER VIEWS ONE DELIVERY.
     */
    @Transactional(readOnly = true)
    public DeliveryResponse getMyDelivery(
            Long riderId,
            Long deliveryId
    ) {
        getActiveRider(riderId);

        Delivery delivery =
                getRiderDelivery(
                        riderId,
                        deliveryId
                );

        return DeliveryResponse.from(delivery);
    }

    /*
     * RIDER UPDATES A DELIVERY STATUS.
     */
    @Transactional
    public DeliveryResponse updateStatus(
            Long riderId,
            Long deliveryId,
            DeliveryStatusRequest request
    ) {
        getActiveRider(riderId);

        if (
                request == null ||
                request.status() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delivery status is required"
            );
        }

        Delivery delivery =
                getRiderDelivery(
                        riderId,
                        deliveryId
                );

        DeliveryStatus currentStatus =
                delivery.getStatus();

        DeliveryStatus newStatus =
                request.status();

        validateStatusChange(
                currentStatus,
                newStatus
        );

        if (
                request.note() != null &&
                !request.note().isBlank()
        ) {
            delivery.setDeliveryNote(
                    request.note().trim()
            );
        }

        switch (newStatus) {

            case ACCEPTED -> {
                delivery.setAcceptedAt(
                        LocalDateTime.now()
                );

                delivery.setFailureReason(null);
            }

            case PICKED_UP -> {
                delivery.setPickedUpAt(
                        LocalDateTime.now()
                );

                delivery.setFailureReason(null);
            }

            case OUT_FOR_DELIVERY -> {
                delivery.setOutForDeliveryAt(
                        LocalDateTime.now()
                );

                delivery.setFailureReason(null);

                Order order =
                        delivery.getOrder();

                order.setStatus(
                        OrderStatus.OUT_FOR_DELIVERY
                );

                orderRepository.save(order);
            }

            case DELIVERED -> {
                delivery.setDeliveredAt(
                        LocalDateTime.now()
                );

                delivery.setFailureReason(null);

                markOrderItemDelivered(
                        delivery.getOrderItem()
                );
            }

            case FAILED -> {
                String failureReason =
                        clean(
                                request.failureReason()
                        );

                if (failureReason == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Failure reason is required"
                    );
                }

                delivery.setFailureReason(
                        failureReason
                );
            }

            case CANCELLED -> {
                delivery.setFailureReason(
                        clean(
                                request.failureReason()
                        )
                );
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The rider cannot set this delivery status"
            );
        }

        delivery.setStatus(newStatus);

        Delivery savedDelivery =
                deliveryRepository.save(delivery);

        sendCustomerDeliveryNotification(
                savedDelivery,
                newStatus
        );

        return DeliveryResponse.from(
                savedDelivery
        );
    }

    /*
     * MARK THE CONNECTED ORDER ITEM AS DELIVERED.
     */
    private void markOrderItemDelivered(
            OrderItem orderItem
    ) {
        if (orderItem == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The delivery does not have a valid order item"
            );
        }

        orderItem.setStatus(
                OrderItemStatus.DELIVERED
        );

        orderItem.setRejectionReason(null);

        OrderItem savedItem =
                orderItemRepository.save(orderItem);

        recalculateOrderStatus(
                savedItem.getOrder()
        );
    }

    /*
     * RECALCULATE THE MAIN ORDER STATUS.
     *
     * One order may contain several items from
     * different sellers or kitchens.
     */
    private void recalculateOrderStatus(
            Order order
    ) {
        List<OrderItem> orderItems =
                orderItemRepository
                        .findAllByOrder_IdOrderByCreatedAtAsc(
                                order.getId()
                        );

        List<OrderItem> activeItems =
                orderItems.stream()
                        .filter(item ->
                                item.getStatus()
                                        != OrderItemStatus.REJECTED
                                &&
                                item.getStatus()
                                        != OrderItemStatus.CANCELLED
                        )
                        .toList();

        if (activeItems.isEmpty()) {
            order.setStatus(
                    OrderStatus.REJECTED
            );

            orderRepository.save(order);
            return;
        }

        boolean allDelivered =
                activeItems.stream()
                        .allMatch(item ->
                                item.getStatus()
                                        == OrderItemStatus.DELIVERED
                        );

        if (allDelivered) {
            order.setStatus(
                    OrderStatus.DELIVERED
            );
        } else {
            order.setStatus(
                    OrderStatus.OUT_FOR_DELIVERY
            );
        }

        orderRepository.save(order);
    }

    /*
     * SEND CUSTOMER NOTIFICATIONS FOR IMPORTANT
     * RIDER DELIVERY UPDATES.
     */
    private void sendCustomerDeliveryNotification(
            Delivery delivery,
            DeliveryStatus status
    ) {
        Order order =
                delivery.getOrder();

        switch (status) {

            case PICKED_UP ->
                    notificationService.sendNotification(
                            order.getCustomer(),
                            NotificationType.ORDER_PICKED_UP,
                            "Order picked up",
                            "The rider has picked up your order "
                                    + order.getOrderNumber()
                                    + ".",
                            "DELIVERY",
                            delivery.getId(),
                            "/customer/orders/"
                                    + order.getId(),
                            null
                    );

            case OUT_FOR_DELIVERY ->
                    notificationService.sendNotification(
                            order.getCustomer(),
                            NotificationType.OUT_FOR_DELIVERY,
                            "Order out for delivery",
                            "Your order "
                                    + order.getOrderNumber()
                                    + " is now on the way.",
                            "DELIVERY",
                            delivery.getId(),
                            "/customer/orders/"
                                    + order.getId(),
                            null
                    );

            case DELIVERED ->
                    notificationService.sendNotification(
                            order.getCustomer(),
                            NotificationType.ORDER_DELIVERED,
                            "Order delivered",
                            "Your order "
                                    + order.getOrderNumber()
                                    + " has been delivered successfully.",
                            "DELIVERY",
                            delivery.getId(),
                            "/customer/orders/"
                                    + order.getId(),
                            null
                    );

            default -> {
                // No customer notification is required
                // for the other rider status changes.
            }
        }
    }

    /*
     * VALIDATE DELIVERY STATUS MOVEMENT.
     */
    private void validateStatusChange(
            DeliveryStatus currentStatus,
            DeliveryStatus newStatus
    ) {
        boolean valid =
                switch (currentStatus) {

                    case ASSIGNED ->
                            newStatus
                                    == DeliveryStatus.ACCEPTED
                            ||
                            newStatus
                                    == DeliveryStatus.FAILED
                            ||
                            newStatus
                                    == DeliveryStatus.CANCELLED;

                    case ACCEPTED ->
                            newStatus
                                    == DeliveryStatus.PICKED_UP
                            ||
                            newStatus
                                    == DeliveryStatus.FAILED
                            ||
                            newStatus
                                    == DeliveryStatus.CANCELLED;

                    case PICKED_UP ->
                            newStatus
                                    == DeliveryStatus.OUT_FOR_DELIVERY
                            ||
                            newStatus
                                    == DeliveryStatus.FAILED;

                    case OUT_FOR_DELIVERY ->
                            newStatus
                                    == DeliveryStatus.DELIVERED
                            ||
                            newStatus
                                    == DeliveryStatus.FAILED;

                    default -> false;
                };

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid delivery status change from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }
    }

    /*
     * GET A DELIVERY BELONGING TO THE
     * LOGGED-IN RIDER.
     */
    private Delivery getRiderDelivery(
            Long riderId,
            Long deliveryId
    ) {
        return deliveryRepository
                .findByIdAndRider_Id(
                        deliveryId,
                        riderId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Delivery not found"
                        )
                );
    }

    /*
     * CONFIRM THE ACCOUNT IS AN ACTIVE RIDER.
     */
    private User getActiveRider(
            Long riderId
    ) {
        User rider =
                userRepository.findById(riderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Rider account not found"
                                )
                        );

        if (rider.getRole() != Role.RIDER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only riders can use this endpoint"
            );
        }

        if (
                rider.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your rider account is not active"
            );
        }

        return rider;
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