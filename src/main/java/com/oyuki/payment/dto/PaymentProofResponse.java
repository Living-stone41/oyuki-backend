package com.oyuki.payment.dto;

import com.oyuki.payment.entity.PaymentProof;
import com.oyuki.payment.enums.PaymentProofStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentProofResponse(

        Long id,

        Long orderId,
        String orderNumber,

        Long customerId,
        String customerName,
        String customerEmail,
        String customerPhone,

        BigDecimal amount,

        String senderBankName,
        String senderAccountName,
        String transactionReference,
        LocalDateTime paymentDate,

        String receiptUrl,
        String originalFileName,
        String fileContentType,
        Long fileSize,

        PaymentProofStatus status,

        Long reviewedById,
        String reviewedByName,
        String adminNote,
        String rejectionReason,
        LocalDateTime reviewedAt,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static PaymentProofResponse from(
            PaymentProof paymentProof
    ) {
        return new PaymentProofResponse(
                paymentProof.getId(),

                paymentProof.getOrder().getId(),
                paymentProof.getOrder().getOrderNumber(),

                paymentProof.getCustomer().getId(),
                paymentProof.getCustomer().getFullName(),
                paymentProof.getCustomer().getEmail(),
                paymentProof.getCustomer().getPhoneNumber(),

                paymentProof.getAmount(),

                paymentProof.getSenderBankName(),
                paymentProof.getSenderAccountName(),
                paymentProof.getTransactionReference(),
                paymentProof.getPaymentDate(),

                paymentProof.getReceiptUrl(),
                paymentProof.getOriginalFileName(),
                paymentProof.getFileContentType(),
                paymentProof.getFileSize(),

                paymentProof.getStatus(),

                paymentProof.getReviewedBy() == null
                        ? null
                        : paymentProof.getReviewedBy().getId(),

                paymentProof.getReviewedBy() == null
                        ? null
                        : paymentProof.getReviewedBy().getFullName(),

                paymentProof.getAdminNote(),
                paymentProof.getRejectionReason(),
                paymentProof.getReviewedAt(),

                paymentProof.getCreatedAt(),
                paymentProof.getUpdatedAt()
        );
    }
}