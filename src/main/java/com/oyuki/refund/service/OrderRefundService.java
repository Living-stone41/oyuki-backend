package com.oyuki.refund.service;

import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.order.entity.Order;
import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.enums.PaymentStatus;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.refund.dto.CancelRefundRequest;
import com.oyuki.refund.dto.CompleteRefundRequest;
import com.oyuki.refund.dto.CreateRefundRequest;
import com.oyuki.refund.dto.FailRefundRequest;
import com.oyuki.refund.dto.RefundResponse;
import com.oyuki.refund.entity.OrderRefund;
import com.oyuki.refund.enums.RefundStatus;
import com.oyuki.refund.enums.RefundType;
import com.oyuki.refund.repository.OrderRefundRepository;
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
public class OrderRefundService {

    private final OrderRefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public OrderRefundService(
            OrderRefundRepository refundRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /*
     * =========================================================
     * CREATE REFUND
     * =========================================================
     */

    @Transactional
    public RefundResponse createRefund(
            Long adminId,
            Long orderId,
            CreateRefundRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        validateCreateRequest(
                order,
                request
        );

        boolean activeRefundExists =
                refundRepository
                        .existsByOrder_IdAndStatusIn(
                                orderId,
                                List.of(
                                        RefundStatus.PENDING,
                                        RefundStatus.PROCESSING
                                )
                        );

        if (activeRefundExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order already has a pending or processing refund"
            );
        }

        BigDecimal remainingAmount =
                calculateRemainingRefundableAmount(
                        order
                );

        validateRefundAmount(
                request,
                remainingAmount
        );

        OrderRefund refund =
                OrderRefund.builder()
                        .order(order)
                        .customer(order.getCustomer())
                        .refundType(request.refundType())
                        .amount(request.amount())
                        .reason(request.reason().trim())
                        .status(RefundStatus.PENDING)
                        .adminNote(clean(request.note()))
                        .createdBy(admin)
                        .build();

        OrderRefund savedRefund =
                refundRepository.save(refund);

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.REFUND_CREATED,
                "Refund created",
                "A "
                        + savedRefund.getRefundType().name().toLowerCase()
                        + " refund of ₦"
                        + savedRefund.getAmount()
                        + " has been created for order "
                        + order.getOrderNumber()
                        + ".",
                "ORDER_REFUND",
                savedRefund.getId(),
                "/customer/orders/" + order.getId(),
                null
        );

        return RefundResponse.from(savedRefund);
    }

    /*
     * =========================================================
     * VIEW REFUNDS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<RefundResponse> getAllRefunds(
            Long adminId,
            RefundStatus status
    ) {
        getActiveAdmin(adminId);

        List<OrderRefund> refunds;

        if (status == null) {
            refunds =
                    refundRepository
                            .findAllByOrderByCreatedAtDesc();
        } else {
            refunds =
                    refundRepository
                            .findAllByStatusOrderByCreatedAtDesc(
                                    status
                            );
        }

        return refunds.stream()
                .map(RefundResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefund(
            Long adminId,
            Long refundId
    ) {
        getActiveAdmin(adminId);

        return RefundResponse.from(
                getRefundEntity(refundId)
        );
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getOrderRefunds(
            Long adminId,
            Long orderId
    ) {
        getActiveAdmin(adminId);

        orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );

        return refundRepository
                .findAllByOrder_IdOrderByCreatedAtDesc(
                        orderId
                )
                .stream()
                .map(RefundResponse::from)
                .toList();
    }

    /*
     * =========================================================
     * START PROCESSING
     * =========================================================
     */

    @Transactional
    public RefundResponse startProcessing(
            Long adminId,
            Long refundId
    ) {
        User admin = getActiveAdmin(adminId);

        OrderRefund refund =
                getRefundEntity(refundId);

        requireStatus(
                refund,
                RefundStatus.PENDING,
                "Only pending refunds can start processing"
        );

        refund.setStatus(
                RefundStatus.PROCESSING
        );

        refund.setProcessedBy(admin);

        refund.setProcessingStartedAt(
                LocalDateTime.now()
        );

        refund.setFailureReason(null);

        OrderRefund savedRefund =
                refundRepository.save(refund);

        notificationService.sendNotification(
                refund.getCustomer(),
                NotificationType.REFUND_PROCESSING,
                "Refund processing",
                "Your refund for order "
                        + refund.getOrder().getOrderNumber()
                        + " is now being processed.",
                "ORDER_REFUND",
                refund.getId(),
                "/customer/orders/"
                        + refund.getOrder().getId(),
                null
        );

        return RefundResponse.from(savedRefund);
    }

    /*
     * =========================================================
     * COMPLETE REFUND
     * =========================================================
     */

    @Transactional
    public RefundResponse completeRefund(
            Long adminId,
            Long refundId,
            CompleteRefundRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        OrderRefund refund =
                getRefundEntity(refundId);

        requireStatus(
                refund,
                RefundStatus.PROCESSING,
                "Only processing refunds can be completed"
        );

        if (
                request == null ||
                request.transactionReference() == null ||
                request.transactionReference().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund transaction reference is required"
            );
        }

        refund.setStatus(
                RefundStatus.COMPLETED
        );

        refund.setTransactionReference(
                request.transactionReference().trim()
        );

        refund.setAdminNote(
                clean(request.note())
        );

        refund.setProcessedBy(admin);

        refund.setCompletedAt(
                LocalDateTime.now()
        );

        refund.setFailureReason(null);

        OrderRefund savedRefund =
                refundRepository.save(refund);

        updateOrderPaymentStatus(
                refund.getOrder()
        );

        notificationService.sendNotification(
                refund.getCustomer(),
                NotificationType.REFUND_COMPLETED,
                "Refund completed",
                "Your refund of ₦"
                        + refund.getAmount()
                        + " for order "
                        + refund.getOrder().getOrderNumber()
                        + " has been completed.",
                "ORDER_REFUND",
                refund.getId(),
                "/customer/orders/"
                        + refund.getOrder().getId(),
                null
        );

        return RefundResponse.from(savedRefund);
    }

    /*
     * =========================================================
     * FAIL REFUND
     * =========================================================
     */

    @Transactional
    public RefundResponse failRefund(
            Long adminId,
            Long refundId,
            FailRefundRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        OrderRefund refund =
                getRefundEntity(refundId);

        requireStatus(
                refund,
                RefundStatus.PROCESSING,
                "Only processing refunds can be marked as failed"
        );

        if (
                request == null ||
                request.reason() == null ||
                request.reason().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failure reason is required"
            );
        }

        refund.setStatus(
                RefundStatus.FAILED
        );

        refund.setFailureReason(
                request.reason().trim()
        );

        refund.setProcessedBy(admin);

        refund.setFailedAt(
                LocalDateTime.now()
        );

        OrderRefund savedRefund =
                refundRepository.save(refund);

        notificationService.sendNotification(
                refund.getCustomer(),
                NotificationType.REFUND_FAILED,
                "Refund delayed",
                "The refund for order "
                        + refund.getOrder().getOrderNumber()
                        + " could not be completed. Oyuki will review it.",
                "ORDER_REFUND",
                refund.getId(),
                "/customer/orders/"
                        + refund.getOrder().getId(),
                null
        );

        return RefundResponse.from(savedRefund);
    }

    /*
     * =========================================================
     * CANCEL REFUND RECORD
     * =========================================================
     */

    @Transactional
    public RefundResponse cancelRefund(
            Long adminId,
            Long refundId,
            CancelRefundRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        OrderRefund refund =
                getRefundEntity(refundId);

        if (
                refund.getStatus() != RefundStatus.PENDING &&
                refund.getStatus() != RefundStatus.PROCESSING
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending or processing refunds can be cancelled"
            );
        }

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

        refund.setStatus(
                RefundStatus.CANCELLED
        );

        refund.setAdminNote(
                request.reason().trim()
        );

        refund.setProcessedBy(admin);

        refund.setCancelledAt(
                LocalDateTime.now()
        );

        OrderRefund savedRefund =
                refundRepository.save(refund);

        notificationService.sendNotification(
                refund.getCustomer(),
                NotificationType.REFUND_CANCELLED,
                "Refund cancelled",
                "The refund for order "
                        + refund.getOrder().getOrderNumber()
                        + " was cancelled. Reason: "
                        + request.reason().trim(),
                "ORDER_REFUND",
                refund.getId(),
                "/customer/orders/"
                        + refund.getOrder().getId(),
                null
        );

        return RefundResponse.from(savedRefund);
    }

    /*
     * =========================================================
     * PAYMENT STATUS UPDATE
     * =========================================================
     */

    private void updateOrderPaymentStatus(
            Order order
    ) {
        BigDecimal completedRefundAmount =
                refundRepository
                        .findAllByOrder_IdOrderByCreatedAtDesc(
                                order.getId()
                        )
                        .stream()
                        .filter(refund ->
                                refund.getStatus()
                                        == RefundStatus.COMPLETED
                        )
                        .map(OrderRefund::getAmount)
                        .filter(amount -> amount != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalAmount =
                order.getTotalAmount() == null
                        ? BigDecimal.ZERO
                        : order.getTotalAmount();

        if (
                completedRefundAmount.compareTo(
                        totalAmount
                ) >= 0
        ) {
            order.setPaymentStatus(
                    PaymentStatus.REFUNDED
            );

            if (
                    order.getStatus()
                            != OrderStatus.DELIVERED
            ) {
                order.setStatus(
                        OrderStatus.CANCELLED
                );
            }
        } else {
            order.setPaymentStatus(
                    PaymentStatus.PARTIALLY_REFUNDED
            );
        }

        orderRepository.save(order);
    }

    /*
     * =========================================================
     * REFUND CALCULATIONS
     * =========================================================
     */

    private BigDecimal calculateRemainingRefundableAmount(
            Order order
    ) {
        BigDecimal totalAmount =
                order.getTotalAmount() == null
                        ? BigDecimal.ZERO
                        : order.getTotalAmount();

        BigDecimal alreadyCompleted =
                refundRepository
                        .findAllByOrder_IdOrderByCreatedAtDesc(
                                order.getId()
                        )
                        .stream()
                        .filter(refund ->
                                refund.getStatus()
                                        == RefundStatus.COMPLETED
                        )
                        .map(OrderRefund::getAmount)
                        .filter(amount -> amount != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return totalAmount.subtract(
                alreadyCompleted
        );
    }

    private void validateRefundAmount(
            CreateRefundRequest request,
            BigDecimal remainingAmount
    ) {
        BigDecimal amount =
                request.amount();

        if (
                amount.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount must be greater than zero"
            );
        }

        if (
                amount.compareTo(
                        remainingAmount
                ) > 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount cannot exceed the remaining refundable amount of ₦"
                            + remainingAmount
            );
        }

        if (
                request.refundType()
                        == RefundType.FULL &&
                amount.compareTo(
                        remainingAmount
                ) != 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A full refund must equal the remaining refundable amount of ₦"
                            + remainingAmount
            );
        }

        if (
                request.refundType()
                        == RefundType.PARTIAL &&
                amount.compareTo(
                        remainingAmount
                ) == 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use FULL when refunding the entire remaining amount"
            );
        }
    }

    private void validateCreateRequest(
            Order order,
            CreateRefundRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund request is required"
            );
        }

        if (order.getCustomer() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The order does not have a customer"
            );
        }

        if (
                order.getPaymentStatus()
                        != PaymentStatus.PAID &&
                order.getPaymentStatus()
                        != PaymentStatus.PARTIALLY_REFUNDED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only paid or partially refunded orders can be refunded"
            );
        }

        if (request.refundType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund type is required"
            );
        }

        if (request.amount() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount is required"
            );
        }

        if (
                request.reason() == null ||
                request.reason().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund reason is required"
            );
        }
    }

    /*
     * =========================================================
     * ADMIN AND REFUND VALIDATION
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
                    "Only administrators can manage refunds"
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

    private OrderRefund getRefundEntity(
            Long refundId
    ) {
        return refundRepository
                .findById(refundId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Refund not found"
                        )
                );
    }

    private void requireStatus(
            OrderRefund refund,
            RefundStatus requiredStatus,
            String message
    ) {
        if (
                refund.getStatus()
                        != requiredStatus
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
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
    @Transactional
public RefundResponse createFullRefundForCancellation(
        Long adminId,
        Long orderId,
        String reason,
        String note
) {
    Order order =
            orderRepository
                    .findById(orderId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Order not found"
                            )
                    );

    BigDecimal remainingAmount =
            calculateRemainingRefundableAmount(order);

    if (
            remainingAmount.compareTo(
                    BigDecimal.ZERO
            ) <= 0
    ) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This order has no refundable amount remaining"
        );
    }

    CreateRefundRequest request =
            new CreateRefundRequest(
                    RefundType.FULL,
                    remainingAmount,
                    reason,
                    note
            );

    return createRefund(
            adminId,
            orderId,
            request
    );
}
}