package com.oyuki.tracking.service;

import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.OrderItemStatus;
import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.enums.PaymentStatus;
import com.oyuki.order.repository.OrderItemRepository;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.tracking.dto.AssignOrderRiderRequest;
import com.oyuki.tracking.dto.OrderDeliveryResponse;
import com.oyuki.tracking.dto.UpdateDeliveryNoteRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderDeliveryService {

    private final OrderDeliveryRepository orderDeliveryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public OrderDeliveryService(
            OrderDeliveryRepository orderDeliveryRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.orderDeliveryRepository = orderDeliveryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /*
     * =========================================================
     * ADMIN ASSIGNS RIDER
     * =========================================================
     */

    @Transactional
    public OrderDeliveryResponse assignRider(
            Long adminId,
            Long orderId,
            AssignOrderRiderRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        if (
                request == null ||
                request.riderId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rider ID is required"
            );
        }

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        if (
                order.getPaymentStatus()
                        != PaymentStatus.PAID
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment must be confirmed before assigning a rider"
            );
        }

        if (
                order.getStatus()
                        != OrderStatus.READY_FOR_DELIVERY
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The complete order must be received by Oyuki before assigning a rider"
            );
        }

        if (
                orderDeliveryRepository
                        .existsByOrder_Id(orderId)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A delivery already exists for this order"
            );
        }

        User rider =
                getActiveRider(
                        request.riderId()
                );

        String trackingNumber =
                generateTrackingNumber();

        OrderDelivery delivery =
                OrderDelivery.builder()
                        .order(order)
                        .rider(rider)
                        .assignedBy(admin)
                        .trackingNumber(trackingNumber)
                        .status(
                                OrderDeliveryStatus.ASSIGNED
                        )
                        .adminNote(
                                clean(request.note())
                        )
                        .assignedAt(
                                LocalDateTime.now()
                        )
                        .build();

        OrderDelivery savedDelivery =
                orderDeliveryRepository.save(
                        delivery
                );

        sendRiderAssignedNotifications(
                savedDelivery
        );

        return OrderDeliveryResponse.from(
                savedDelivery
        );
    }

    /*
     * =========================================================
     * ADMIN VIEWS DELIVERIES
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderDeliveryResponse>
    getAdminDeliveries(
            Long adminId,
            OrderDeliveryStatus status
    ) {
        getActiveAdmin(adminId);

        List<OrderDelivery> deliveries;

        if (status == null) {
            deliveries =
                    orderDeliveryRepository
                            .findAllByOrderByCreatedAtDesc();
        } else {
            deliveries =
                    orderDeliveryRepository
                            .findAllByStatusOrderByCreatedAtDesc(
                                    status
                            );
        }

        return deliveries.stream()
                .map(OrderDeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDeliveryResponse
    getAdminDelivery(
            Long adminId,
            Long deliveryId
    ) {
        getActiveAdmin(adminId);

        OrderDelivery delivery =
                getDelivery(deliveryId);

        return OrderDeliveryResponse.from(
                delivery
        );
    }

    @Transactional(readOnly = true)
    public OrderDeliveryResponse
    getAdminOrderDelivery(
            Long adminId,
            Long orderId
    ) {
        getActiveAdmin(adminId);

        OrderDelivery delivery =
                orderDeliveryRepository
                        .findByOrder_Id(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Delivery not found for this order"
                                )
                        );

        return OrderDeliveryResponse.from(
                delivery
        );
    }

    /*
     * =========================================================
     * RIDER VIEWS ASSIGNED DELIVERIES
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderDeliveryResponse>
    getRiderDeliveries(
            Long riderId,
            OrderDeliveryStatus status
    ) {
        getActiveRider(riderId);

        List<OrderDelivery> deliveries;

        if (status == null) {
            deliveries =
                    orderDeliveryRepository
                            .findAllByRider_IdOrderByCreatedAtDesc(
                                    riderId
                            );
        } else {
            deliveries =
                    orderDeliveryRepository
                            .findAllByRider_IdAndStatusOrderByCreatedAtDesc(
                                    riderId,
                                    status
                            );
        }

        return deliveries.stream()
                .map(OrderDeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDeliveryResponse
    getRiderDelivery(
            Long riderId,
            Long deliveryId
    ) {
        getActiveRider(riderId);

        OrderDelivery delivery =
                findRiderDelivery(
                        riderId,
                        deliveryId
                );

        return OrderDeliveryResponse.from(
                delivery
        );
    }

    /*
     * =========================================================
     * RIDER ACCEPTS DELIVERY
     * =========================================================
     */

    @Transactional
    public OrderDeliveryResponse acceptDelivery(
            Long riderId,
            Long deliveryId,
            UpdateDeliveryNoteRequest request
    ) {
        getActiveRider(riderId);

        OrderDelivery delivery =
                findRiderDelivery(
                        riderId,
                        deliveryId
                );

        requireStatus(
                delivery,
                OrderDeliveryStatus.ASSIGNED,
                "Only assigned deliveries can be accepted"
        );

        delivery.setStatus(
                OrderDeliveryStatus.ACCEPTED
        );

        delivery.setAcceptedAt(
                LocalDateTime.now()
        );

        updateRiderNote(
                delivery,
                request
        );

        OrderDelivery savedDelivery =
                orderDeliveryRepository.save(
                        delivery
                );

        return OrderDeliveryResponse.from(
                savedDelivery
        );
    }

    /*
     * =========================================================
     * RIDER PICKS UP ORDER FROM OYUKI HUB
     * =========================================================
     */

    @Transactional
    public OrderDeliveryResponse markPickedUp(
            Long riderId,
            Long deliveryId,
            UpdateDeliveryNoteRequest request
    ) {
        getActiveRider(riderId);

        OrderDelivery delivery =
                findRiderDelivery(
                        riderId,
                        deliveryId
                );

        requireStatus(
                delivery,
                OrderDeliveryStatus.ACCEPTED,
                "Only accepted deliveries can be marked as picked up"
        );

        delivery.setStatus(
                OrderDeliveryStatus.PICKED_UP
        );

        delivery.setPickedUpAt(
                LocalDateTime.now()
        );

        updateRiderNote(
                delivery,
                request
        );

        OrderDelivery savedDelivery =
                orderDeliveryRepository.save(
                        delivery
                );

        Order order =
                savedDelivery.getOrder();

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.ORDER_PICKED_UP,
                "Order picked up",
                "The rider has collected your order "
                        + order.getOrderNumber()
                        + " from the Oyuki hub.",
                "ORDER_DELIVERY",
                savedDelivery.getId(),
                "/tracking/"
                        + savedDelivery.getTrackingNumber(),
                null
        );

        return OrderDeliveryResponse.from(
                savedDelivery
        );
    }

    /*
     * =========================================================
     * RIDER STARTS CUSTOMER DELIVERY
     * =========================================================
     */

    @Transactional
    public OrderDeliveryResponse
    markOutForDelivery(
            Long riderId,
            Long deliveryId,
            UpdateDeliveryNoteRequest request
    ) {
        getActiveRider(riderId);

        OrderDelivery delivery =
                findRiderDelivery(
                        riderId,
                        deliveryId
                );

        requireStatus(
                delivery,
                OrderDeliveryStatus.PICKED_UP,
                "Only picked-up deliveries can be marked out for delivery"
        );

        delivery.setStatus(
                OrderDeliveryStatus.OUT_FOR_DELIVERY
        );

        delivery.setOutForDeliveryAt(
                LocalDateTime.now()
        );

        updateRiderNote(
                delivery,
                request
        );

        Order order =
                delivery.getOrder();

        order.setStatus(
                OrderStatus.OUT_FOR_DELIVERY
        );

        orderRepository.save(order);

        OrderDelivery savedDelivery =
                orderDeliveryRepository.save(
                        delivery
                );

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.OUT_FOR_DELIVERY,
                "Order out for delivery",
                "Your order "
                        + order.getOrderNumber()
                        + " is on its way.",
                "ORDER_DELIVERY",
                savedDelivery.getId(),
                "/tracking/"
                        + savedDelivery.getTrackingNumber(),
                null
        );

        return OrderDeliveryResponse.from(
                savedDelivery
        );
    }

    /*
     * =========================================================
     * RIDER COMPLETES DELIVERY
     * =========================================================
     */

    @Transactional
    public OrderDeliveryResponse markDelivered(
            Long riderId,
            Long deliveryId,
            UpdateDeliveryNoteRequest request
    ) {
        getActiveRider(riderId);

        OrderDelivery delivery =
                findRiderDelivery(
                        riderId,
                        deliveryId
                );

        requireStatus(
                delivery,
                OrderDeliveryStatus.OUT_FOR_DELIVERY,
                "Only deliveries that are out for delivery can be completed"
        );

        delivery.setStatus(
                OrderDeliveryStatus.DELIVERED
        );

        delivery.setDeliveredAt(
                LocalDateTime.now()
        );

        updateRiderNote(
                delivery,
                request
        );

        Order order =
                delivery.getOrder();

        order.setStatus(
                OrderStatus.DELIVERED
        );

        orderRepository.save(order);

        markOrderItemsDelivered(
                order.getId()
        );

        OrderDelivery savedDelivery =
                orderDeliveryRepository.save(
                        delivery
                );

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.ORDER_DELIVERED,
                "Order delivered",
                "Your order "
                        + order.getOrderNumber()
                        + " has been delivered successfully.",
                "ORDER_DELIVERY",
                savedDelivery.getId(),
                "/tracking/"
                        + savedDelivery.getTrackingNumber(),
                null
        );

        return OrderDeliveryResponse.from(
                savedDelivery
        );
    }

    /*
     * =========================================================
     * CUSTOMER TRACKING
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<OrderDeliveryResponse>
    getCustomerDeliveries(
            Long customerId
    ) {
        getActiveCustomer(customerId);

        return orderDeliveryRepository
                .findAllByOrder_Customer_IdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(OrderDeliveryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDeliveryResponse
    getCustomerOrderDelivery(
            Long customerId,
            Long orderId
    ) {
        getActiveCustomer(customerId);

        OrderDelivery delivery =
                orderDeliveryRepository
                        .findByOrder_IdAndOrder_Customer_Id(
                                orderId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Delivery not found"
                                )
                        );

        return OrderDeliveryResponse.from(
                delivery
        );
    }

    @Transactional(readOnly = true)
    public OrderDeliveryResponse
    trackByTrackingNumber(
            Long customerId,
            String trackingNumber
    ) {
        getActiveCustomer(customerId);

        if (
                trackingNumber == null ||
                trackingNumber.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tracking number is required"
            );
        }

        OrderDelivery delivery =
                orderDeliveryRepository
                        .findByTrackingNumberIgnoreCase(
                                trackingNumber.trim()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Tracking number not found"
                                )
                        );

        if (
                delivery.getOrder() == null ||
                delivery.getOrder().getCustomer() == null ||
                !delivery.getOrder()
                        .getCustomer()
                        .getId()
                        .equals(customerId)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view this delivery"
            );
        }

        return OrderDeliveryResponse.from(
                delivery
        );
    }

    /*
     * =========================================================
     * ORDER ITEM COMPLETION
     * =========================================================
     */

    private void markOrderItemsDelivered(
            Long orderId
    ) {
        List<OrderItem> orderItems =
                orderItemRepository
                        .findAllByOrder_IdOrderByCreatedAtAsc(
                                orderId
                        );

        for (OrderItem item : orderItems) {
            if (
                    item.getStatus()
                            != OrderItemStatus.REJECTED &&
                    item.getStatus()
                            != OrderItemStatus.CANCELLED
            ) {
                item.setStatus(
                        OrderItemStatus.DELIVERED
                );
            }
        }

        orderItemRepository.saveAll(
                orderItems
        );
    }

    /*
     * =========================================================
     * NOTIFICATIONS
     * =========================================================
     */

    private void sendRiderAssignedNotifications(
            OrderDelivery delivery
    ) {
        Order order =
                delivery.getOrder();

        User rider =
                delivery.getRider();

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.RIDER_ASSIGNED,
                "Rider assigned",
                rider.getFullName()
                        + " has been assigned to deliver order "
                        + order.getOrderNumber()
                        + ". Tracking number: "
                        + delivery.getTrackingNumber(),
                "ORDER_DELIVERY",
                delivery.getId(),
                "/tracking/"
                        + delivery.getTrackingNumber(),
                null
        );

        notificationService.sendNotification(
                rider,
                NotificationType.RIDER_ASSIGNED,
                "New delivery assigned",
                "You have been assigned to deliver order "
                        + order.getOrderNumber()
                        + " from the Oyuki hub.",
                "ORDER_DELIVERY",
                delivery.getId(),
                "/rider/deliveries/"
                        + delivery.getId(),
                null
        );
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

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
                    "Only administrators can assign riders"
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

    private User getActiveRider(
            Long riderId
    ) {
        User rider =
                userRepository
                        .findById(riderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Rider account not found"
                                )
                        );

        if (rider.getRole() != Role.RIDER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected account is not a rider"
            );
        }

        if (
                rider.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected rider is not active"
            );
        }

        return rider;
    }

    private User getActiveCustomer(
            Long customerId
    ) {
        User customer =
                userRepository
                        .findById(customerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer account not found"
                                )
                        );

        if (customer.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only customers can view customer tracking"
            );
        }

        if (
                customer.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Customer account is not active"
            );
        }

        return customer;
    }

    private OrderDelivery getDelivery(
            Long deliveryId
    ) {
        return orderDeliveryRepository
                .findById(deliveryId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Delivery not found"
                        )
                );
    }

    private OrderDelivery findRiderDelivery(
            Long riderId,
            Long deliveryId
    ) {
        return orderDeliveryRepository
                .findByIdAndRider_Id(
                        deliveryId,
                        riderId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Delivery not found or not assigned to you"
                        )
                );
    }

    private void requireStatus(
            OrderDelivery delivery,
            OrderDeliveryStatus expectedStatus,
            String message
    ) {
        if (
                delivery.getStatus()
                        != expectedStatus
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
    }

    private void updateRiderNote(
            OrderDelivery delivery,
            UpdateDeliveryNoteRequest request
    ) {
        if (
                request != null &&
                request.note() != null &&
                !request.note().isBlank()
        ) {
            delivery.setRiderNote(
                    request.note().trim()
            );
        }
    }

    private String generateTrackingNumber() {
        String trackingNumber;

        do {
            String datePart =
                    LocalDate.now()
                            .format(
                                    DateTimeFormatter
                                            .ofPattern("yyyyMMdd")
                            );

            String randomPart =
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 8)
                            .toUpperCase();

            trackingNumber =
                    "OYU-"
                            + datePart
                            + "-"
                            + randomPart;

        } while (
                orderDeliveryRepository
                        .existsByTrackingNumberIgnoreCase(
                                trackingNumber
                        )
        );

        return trackingNumber;
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