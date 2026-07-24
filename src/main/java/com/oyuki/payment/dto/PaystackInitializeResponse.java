package com.oyuki.payment.dto;

public record PaystackInitializeResponse(
        Long orderId,
        String orderNumber,
        String authorizationUrl,
        String accessCode,
        String reference,
        Long amount,
        String currency,
        String publicKey
) {
}
