package com.oyuki.payment.config;

public record PaystackProperties(
        String secretKey,
        String publicKey,
        String baseUrl,
        String currency,
        String callbackUrl
) {

    public boolean hasSecretKey() {
        return secretKey != null &&
                !secretKey.isBlank();
    }

    public boolean hasPublicKey() {
        return publicKey != null &&
                !publicKey.isBlank();
    }

    public boolean isConfigured() {
        return hasSecretKey() && hasPublicKey();
    }
}
