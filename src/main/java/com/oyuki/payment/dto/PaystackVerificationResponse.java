package com.oyuki.payment.dto;

import com.oyuki.order.enums.PaymentStatus;

public record PaystackVerificationResponse(
        Long orderId,
        String orderNumber,
        String reference,
        String gatewayStatus,
        String gatewayResponse,
        Long amount,
        String currency,
        PaymentStatus paymentStatus
) {
}
