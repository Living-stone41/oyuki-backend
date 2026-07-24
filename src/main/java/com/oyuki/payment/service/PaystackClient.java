package com.oyuki.payment.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Component
public class PaystackClient {

    private final RestClient paystackRestClient;

    public PaystackClient(
            RestClient paystackRestClient
    ) {
        this.paystackRestClient =
                paystackRestClient;
    }

    public PaystackApiResponse initializeTransaction(
            PaystackInitializeTransactionRequest request
    ) {
        return callPaystack(() ->
                paystackRestClient
                        .post()
                        .uri("/transaction/initialize")
                        .body(request)
                        .retrieve()
                        .body(PaystackApiResponse.class)
        );
    }

    public PaystackApiResponse verifyTransaction(
            String reference
    ) {
        return callPaystack(() ->
                paystackRestClient
                        .get()
                        .uri(
                                "/transaction/verify/{reference}",
                                reference
                        )
                        .retrieve()
                        .body(PaystackApiResponse.class)
        );
    }

    private PaystackApiResponse callPaystack(
            PaystackCall call
    ) {
        try {
            PaystackApiResponse response =
                    call.execute();

            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Paystack returned an empty response"
                );
            }

            if (!response.status()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        response.message() == null
                                ? "Paystack request failed"
                                : response.message()
                );
            }

            return response;

        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Paystack request failed with status "
                            + exception.getStatusCode().value(),
                    exception
            );

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach Paystack",
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface PaystackCall {
        PaystackApiResponse execute();
    }

    public record PaystackApiResponse(
            boolean status,
            String message,
            JsonNode data
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaystackInitializeTransactionRequest(
            String email,
            Long amount,
            String currency,
            String reference,
            String callback_url,
            Map<String, Object> metadata,
            List<String> channels
    ) {
    }
}
