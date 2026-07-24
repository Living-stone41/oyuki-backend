package com.oyuki.payment.dto;

public record PaystackConfigResponse(
        String publicKey,
        String currency,
        boolean configured
) {
}
