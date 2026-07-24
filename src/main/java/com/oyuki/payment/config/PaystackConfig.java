package com.oyuki.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class PaystackConfig {

    @Bean
    public PaystackProperties paystackProperties(
            @Value("${paystack.secret-key:}")
            String secretKey,

            @Value("${paystack.public-key:}")
            String publicKey,

            @Value("${paystack.base-url:https://api.paystack.co}")
            String baseUrl,

            @Value("${paystack.currency:NGN}")
            String currency,

            @Value("${paystack.callback-url:}")
            String callbackUrl
    ) {
        return new PaystackProperties(
                clean(secretKey),
                clean(publicKey),
                clean(baseUrl),
                clean(currency),
                clean(callbackUrl)
        );
    }

    @Bean
    public RestClient paystackRestClient(
            RestClient.Builder builder,
            PaystackProperties properties
    ) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.secretKey()
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .build();
    }

    private String clean(
            String value
    ) {
        return value == null
                ? ""
                : value.trim();
    }
}
