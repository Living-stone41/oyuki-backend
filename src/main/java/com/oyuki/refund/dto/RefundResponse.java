package com.oyuki.refund.dto;

import com.oyuki.refund.entity.OrderRefund;
import com.oyuki.refund.enums.RefundStatus;
import com.oyuki.refund.enums.RefundType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(

        Long id,

        Long orderId,
        String orderNumber,

        Long customerId,
        String customerName,
        String customerEmail,

        RefundType refundType,
        BigDecimal amount,
        String reason,
        RefundStatus status,

        String transactionReference,
        String adminNote,
        String failureReason,

        Long createdById,
        String createdByName,

        Long processedById,
        String processedByName,

        LocalDateTime processingStartedAt,
        LocalDateTime completedAt,
        LocalDateTime failedAt,
        LocalDateTime cancelledAt,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static RefundResponse from(
            OrderRefund refund
    ) {
        return new RefundResponse(
                refund.getId(),

                refund.getOrder().getId(),
                refund.getOrder().getOrderNumber(),

                refund.getCustomer().getId(),
                refund.getCustomer().getFullName(),
                refund.getCustomer().getEmail(),

                refund.getRefundType(),
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),

                refund.getTransactionReference(),
                refund.getAdminNote(),
                refund.getFailureReason(),

                refund.getCreatedBy().getId(),
                refund.getCreatedBy().getFullName(),

                refund.getProcessedBy() == null
                        ? null
                        : refund.getProcessedBy().getId(),

                refund.getProcessedBy() == null
                        ? null
                        : refund.getProcessedBy().getFullName(),

                refund.getProcessingStartedAt(),
                refund.getCompletedAt(),
                refund.getFailedAt(),
                refund.getCancelledAt(),

                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}