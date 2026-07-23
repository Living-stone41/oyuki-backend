package com.oyuki.logistics.service;

import com.oyuki.logistics.dto.AssignRiderRequest;
import com.oyuki.logistics.dto.DeliveryResponse;
import com.oyuki.logistics.entity.Delivery;
import com.oyuki.logistics.enums.DeliveryStatus;
import com.oyuki.logistics.repository.DeliveryRepository;
import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.order.dto.OrderItemResponse;
import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.OrderItemStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogisticsService {

    private final DeliveryRepository deliveryRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public LogisticsService(
            DeliveryRepository deliveryRepository,
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /*
     * =========================================================
     * VIEW READY ORDER ITEMS
     * =========================================================
     *
     * These are order items that sellers or kitchens
     * have marked as READY_FOR_PICKUP.
     *
     * Items that already have a delivery record
     * will not be returned.
     */
    @Transactional(readOnly = true)
    public List<OrderItemResponse> getReadyItems(
            Long logisticsAdminId
    ) {
        getActiveLogisticsAdmin(
                logisticsAdminId
        );

        return orderItemRepository
                .findAllByStatusOrderByCreatedAtDesc(
                        OrderItemStatus.READY_FOR_PICKUP
                )
                .stream()
                .filter(orderItem ->
                        !deliveryRepository
                                .existsByOrderItem_Id(
                                        orderItem.getId()
                                )
                )
                .map(OrderItemResponse::from)
                .toList();
    }

    /*
     * =========================================================
     * VIEW DELIVERIES
     * =========================================================
     *
     * If no status is supplied, all deliveries
     * are returned.
     *
     * Example:
     *
     * GET /api/logistics/deliveries
     *
     * GET /api/logistics/deliveries?status=ASSIGNED
     */
    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDeliveries(
            Long logisticsAdminId,
            DeliveryStatus status
    ) {
        getActiveLogisticsAdmin(
                logisticsAdminId
        );

        List<Delivery> deliveries;

        if (status == null) {
            deliveries =
                    deliveryRepository
                            .findAllByOrderByCreatedAtDesc();
        } else {
            deliveries =
                    deliveryRepository
                            .findAllByStatusOrderByCreatedAtDesc(
                                    status
                            );
        }

        return deliveries.stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    /*
     * =========================================================
     * ASSIGN RIDER
     * =========================================================
     *
     * A logistics administrator assigns an active
     * rider to an order item that is ready for pickup.
     */
    @Transactional
    public DeliveryResponse assignRider(
            Long logisticsAdminId,
            Long orderItemId,
            AssignRiderRequest request
    ) {
        User logisticsAdmin =
                getActiveLogisticsAdmin(
                        logisticsAdminId
                );

        validateAssignRiderRequest(request);

        OrderItem orderItem =
                orderItemRepository
                        .findById(orderItemId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order item not found"
                                )
                        );

        if (
                orderItem.getStatus()
                        != OrderItemStatus.READY_FOR_PICKUP
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The order item is not ready for pickup"
            );
        }

        if (
                deliveryRepository
                        .existsByOrderItem_Id(
                                orderItemId
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A delivery already exists for this order item"
            );
        }

        User rider =
                getActiveRider(
                        request.riderId()
                );

        BigDecimal deliveryFee =
                request.deliveryFee() == null
                        ? BigDecimal.ZERO
                        : request.deliveryFee();

        Delivery delivery =
                Delivery.builder()
                        .order(
                                orderItem.getOrder()
                        )
                        .orderItem(orderItem)
                        .rider(rider)
                        .assignedBy(
                                logisticsAdmin
                        )
                        .status(
                                DeliveryStatus.ASSIGNED
                        )
                        .deliveryFee(
                                deliveryFee
                        )
                        .deliveryNote(
                                clean(
                                        request.deliveryNote()
                                )
                        )
                        .assignedAt(
                                LocalDateTime.now()
                        )
                        .build();

        Delivery savedDelivery =
                deliveryRepository.save(
                        delivery
                );

        updateOrderDeliveryFee(
                orderItem.getOrder(),
                deliveryFee
        );

        /*
         * Notify the customer and the rider.
         */
        sendRiderAssignedNotifications(
                savedDelivery
        );

        return DeliveryResponse.from(
                savedDelivery
        );
    }

    /*
     * =========================================================
     * UPDATE ORDER DELIVERY FEE
     * =========================================================
     *
     * One customer order may contain products from
     * different providers, so each assigned delivery
     * fee is added to the main order.
     */
    private void updateOrderDeliveryFee(
            Order order,
            BigDecimal newDeliveryFee
    ) {
        BigDecimal currentDeliveryFee =
                order.getDeliveryFee() == null
                        ? BigDecimal.ZERO
                        : order.getDeliveryFee();

        BigDecimal subtotal =
                order.getSubtotal() == null
                        ? BigDecimal.ZERO
                        : order.getSubtotal();

        BigDecimal feeToAdd =
                newDeliveryFee == null
                        ? BigDecimal.ZERO
                        : newDeliveryFee;

        BigDecimal updatedDeliveryFee =
                currentDeliveryFee.add(
                        feeToAdd
                );

        order.setDeliveryFee(
                updatedDeliveryFee
        );

        order.setTotalAmount(
                subtotal.add(
                        updatedDeliveryFee
                )
        );

        orderRepository.save(order);
    }

    /*
     * =========================================================
     * RIDER ASSIGNED NOTIFICATIONS
     * =========================================================
     */
    private void sendRiderAssignedNotifications(
            Delivery delivery
    ) {
        Order order =
                delivery.getOrder();

        User customer =
                order.getCustomer();

        User rider =
                delivery.getRider();

        String riderName =
                getDisplayName(
                        rider,
                        "A rider"
                );

        /*
         * Notify the customer.
         */
        notificationService.sendNotification(
                customer,
                NotificationType.RIDER_ASSIGNED,
                "Rider assigned",
                riderName
                        + " has been assigned to deliver your order "
                        + order.getOrderNumber()
                        + ".",
                "DELIVERY",
                delivery.getId(),
                "/customer/orders/"
                        + order.getId(),
                null
        );

        /*
         * Notify the rider.
         */
        notificationService.sendNotification(
                rider,
                NotificationType.RIDER_ASSIGNED,
                "New delivery assigned",
                "You have been assigned to deliver order "
                        + order.getOrderNumber()
                        + ".",
                "DELIVERY",
                delivery.getId(),
                "/rider/deliveries/"
                        + delivery.getId(),
                null
        );
    }

    /*
     * =========================================================
     * VALIDATE ASSIGNMENT REQUEST
     * =========================================================
     */
    private void validateAssignRiderRequest(
            AssignRiderRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rider assignment request is required"
            );
        }

        if (request.riderId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rider is required"
            );
        }

        if (
                request.deliveryFee() != null &&
                request.deliveryFee()
                        .compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delivery fee cannot be negative"
            );
        }
    }

    /*
     * =========================================================
     * VALIDATE LOGISTICS ADMIN
     * =========================================================
     */
    private User getActiveLogisticsAdmin(
            Long userId
    ) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Logistics account not found"
                                )
                        );

        if (
                user.getRole()
                        != Role.LOGISTICS_ADMIN
                &&
                user.getRole()
                        != Role.ADMIN
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a logistics administrator can perform this action"
            );
        }

        if (
                user.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The logistics account is not active"
            );
        }

        return user;
    }

    /*
     * =========================================================
     * VALIDATE RIDER
     * =========================================================
     */
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

        if (
                rider.getRole()
                        != Role.RIDER
        ) {
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

    /*
     * =========================================================
     * SMALL HELPERS
     * =========================================================
     */
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

    private String getDisplayName(
            User user,
            String fallback
    ) {
        if (
                user == null ||
                user.getFullName() == null ||
                user.getFullName().isBlank()
        ) {
            return fallback;
        }

        return user.getFullName().trim();
    }
}