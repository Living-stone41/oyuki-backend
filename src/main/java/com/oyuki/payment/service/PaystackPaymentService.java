package com.oyuki.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.order.entity.Order;
import com.oyuki.order.enums.PaymentMethod;
import com.oyuki.order.enums.PaymentStatus;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.payment.config.PaystackProperties;
import com.oyuki.payment.dto.InitializePaystackPaymentRequest;
import com.oyuki.payment.dto.PaystackConfigResponse;
import com.oyuki.payment.dto.PaystackInitializeResponse;
import com.oyuki.payment.dto.PaystackVerificationResponse;
import com.oyuki.payment.dto.VerifyPaystackPaymentRequest;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaystackPaymentService {

    private static final String PAYSTACK_SUCCESS_STATUS =
            "success";

    private static final List<String> DEFAULT_CHANNELS =
            List.of(
                    "card",
                    "bank",
                    "ussd",
                    "qr",
                    "mobile_money",
                    "bank_transfer"
            );

    private final PaystackProperties properties;
    private final PaystackClient paystackClient;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public PaystackPaymentService(
            PaystackProperties properties,
            PaystackClient paystackClient,
            OrderRepository orderRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.paystackClient = paystackClient;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PaystackConfigResponse getConfig() {
        return new PaystackConfigResponse(
                properties.publicKey(),
                properties.currency(),
                properties.isConfigured()
        );
    }

    @Transactional
    public PaystackInitializeResponse initializePayment(
            Long customerId,
            Long orderId,
            InitializePaystackPaymentRequest request
    ) {
        requirePaystackKeys();

        User customer =
                getActiveCustomer(customerId);

        Order order =
                getCustomerPaystackOrder(
                        orderId,
                        customerId
                );

        if (
                customer.getEmail() == null ||
                customer.getEmail().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A verified email address is required for Paystack payment"
            );
        }

        Long amountInMinorUnit =
                toMinorUnit(order.getTotalAmount());

        String reference =
                generateReference(order);

        Map<String, Object> metadata =
                new LinkedHashMap<>();

        metadata.put("orderId", order.getId());
        metadata.put(
                "orderNumber",
                order.getOrderNumber()
        );
        metadata.put("customerId", customer.getId());

        String callbackUrl =
                clean(
                        request == null
                                ? null
                                : request.callbackUrl()
                );

        if (callbackUrl == null) {
            callbackUrl =
                    clean(properties.callbackUrl());
        }

        PaystackClient.PaystackApiResponse response =
                paystackClient.initializeTransaction(
                        new PaystackClient.PaystackInitializeTransactionRequest(
                                customer.getEmail().trim(),
                                amountInMinorUnit,
                                properties.currency(),
                                reference,
                                callbackUrl,
                                metadata,
                                DEFAULT_CHANNELS
                        )
                );

        JsonNode data =
                requireData(response);

        String authorizationUrl =
                text(data, "authorization_url");

        String accessCode =
                text(data, "access_code");

        String returnedReference =
                text(data, "reference");

        if (
                authorizationUrl == null ||
                accessCode == null ||
                returnedReference == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Paystack did not return checkout authorization details"
            );
        }

        return new PaystackInitializeResponse(
                order.getId(),
                order.getOrderNumber(),
                authorizationUrl,
                accessCode,
                returnedReference,
                amountInMinorUnit,
                properties.currency(),
                properties.publicKey()
        );
    }

    @Transactional
    public PaystackVerificationResponse verifyPayment(
            Long customerId,
            Long orderId,
            VerifyPaystackPaymentRequest request
    ) {
        requirePaystackSecretKey();
        getActiveCustomer(customerId);

        Order order =
                getCustomerPaystackOrder(
                        orderId,
                        customerId
                );

        String reference =
                clean(
                        request == null
                                ? null
                                : request.reference()
                );

        if (reference == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Paystack transaction reference is required"
            );
        }

        PaystackClient.PaystackApiResponse response =
                paystackClient.verifyTransaction(
                        reference
                );

        return applyVerifiedPaystackTransaction(
                order,
                requireData(response)
        );
    }

    @Transactional
    public void handleWebhook(
            String payload,
            String signature
    ) {
        requirePaystackSecretKey();

        if (
                payload == null ||
                !isValidSignature(
                        payload,
                        signature
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Paystack webhook signature"
            );
        }

        JsonNode event =
                parsePayload(payload);

        String eventName =
                text(event, "event");

        if (
                !"charge.success".equals(eventName)
        ) {
            return;
        }

        JsonNode data =
                event.path("data");

        Long orderId =
                resolveOrderId(data);

        if (orderId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Paystack webhook is missing order metadata"
            );
        }

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        validatePaystackPaymentMethod(order);
        validatePaystackTransactionData(order, data);
        markOrderPaid(order);
    }

    private PaystackVerificationResponse
    applyVerifiedPaystackTransaction(
            Order order,
            JsonNode data
    ) {
        validatePaystackTransactionData(
                order,
                data
        );

        validateTransactionBelongsToOrder(
                order,
                data
        );

        String gatewayStatus =
                text(data, "status");

        if (
                PAYSTACK_SUCCESS_STATUS
                        .equalsIgnoreCase(gatewayStatus)
        ) {
            markOrderPaid(order);

        } else if (
                "failed".equalsIgnoreCase(
                        gatewayStatus
                )
        ) {
            order.setPaymentStatus(
                    PaymentStatus.FAILED
            );
            orderRepository.save(order);
        }

        return new PaystackVerificationResponse(
                order.getId(),
                order.getOrderNumber(),
                text(data, "reference"),
                gatewayStatus,
                text(data, "gateway_response"),
                data.path("amount")
                        .asLong(0L),
                text(data, "currency"),
                order.getPaymentStatus()
        );
    }

    private void validatePaystackTransactionData(
            Order order,
            JsonNode data
    ) {
        String gatewayStatus =
                text(data, "status");

        if (
                !PAYSTACK_SUCCESS_STATUS
                        .equalsIgnoreCase(gatewayStatus)
        ) {
            return;
        }

        Long expectedAmount =
                toMinorUnit(order.getTotalAmount());

        Long actualAmount =
                data.path("amount")
                        .asLong(-1L);

        if (!expectedAmount.equals(actualAmount)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Paystack amount does not match the order total"
            );
        }

        String actualCurrency =
                text(data, "currency");

        if (
                actualCurrency == null ||
                !properties.currency()
                        .equalsIgnoreCase(
                                actualCurrency
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Paystack currency does not match the configured order currency"
            );
        }
    }

    private void validateTransactionBelongsToOrder(
            Order order,
            JsonNode data
    ) {
        Long transactionOrderId =
                resolveOrderId(data);

        if (
                transactionOrderId == null ||
                !transactionOrderId.equals(
                        order.getId()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Paystack transaction reference does not belong to this order"
            );
        }
    }

    private void markOrderPaid(
            Order order
    ) {
        if (
                order.getPaymentStatus()
                        == PaymentStatus.PAID
        ) {
            return;
        }

        order.setPaymentStatus(
                PaymentStatus.PAID
        );

        orderRepository.save(order);

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.PAYMENT_CONFIRMED,
                "Payment confirmed",
                "Your Paystack payment for order "
                        + order.getOrderNumber()
                        + " has been confirmed.",
                "ORDER",
                order.getId(),
                "/customer/orders/"
                        + order.getId(),
                null
        );
    }

    private Order getCustomerPaystackOrder(
            Long orderId,
            Long customerId
    ) {
        Order order =
                orderRepository
                        .findByIdAndCustomer_Id(
                                orderId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        validatePaystackPaymentMethod(order);

        if (
                order.getPaymentStatus()
                        == PaymentStatus.PAID
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid"
            );
        }

        return order;
    }

    private void validatePaystackPaymentMethod(
            Order order
    ) {
        if (
                order.getPaymentMethod()
                        != PaymentMethod.PAYSTACK &&
                order.getPaymentMethod()
                        != PaymentMethod.CARD
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This order does not use Paystack payment"
            );
        }
    }

    private User getActiveCustomer(
            Long customerId
    ) {
        User customer =
                userRepository
                        .findById(customerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer account not found"
                                )
                        );

        if (
                customer.getRole()
                        != Role.CUSTOMER
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only customers can perform this action"
            );
        }

        if (
                customer.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your customer account is not active"
            );
        }

        return customer;
    }

    private Long toMinorUnit(
            BigDecimal amount
    ) {
        if (
                amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order total must be greater than zero"
            );
        }

        return amount
                .movePointRight(2)
                .setScale(
                        0,
                        RoundingMode.HALF_UP
                )
                .longValueExact();
    }

    private String generateReference(
            Order order
    ) {
        return "OYUKI-"
                + order.getId()
                + "-"
                + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();
    }

    private Long resolveOrderId(
            JsonNode data
    ) {
        Long orderId =
                metadataOrderId(data);

        if (orderId != null) {
            return orderId;
        }

        String reference =
                text(data, "reference");

        if (
                reference == null ||
                !reference.startsWith("OYUKI-")
        ) {
            return null;
        }

        String[] parts =
                reference.split("-");

        if (parts.length < 3) {
            return null;
        }

        try {
            return Long.valueOf(parts[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long metadataOrderId(
            JsonNode data
    ) {
        JsonNode metadata =
                data.path("metadata");

        if (metadata.isTextual()) {
            try {
                metadata =
                        objectMapper.readTree(
                                metadata.asText()
                        );
            } catch (JsonProcessingException exception) {
                return null;
            }
        }

        JsonNode orderId =
                metadata.path("orderId");

        if (
                orderId.isNumber() ||
                orderId.isTextual()
        ) {
            try {
                return Long.valueOf(
                        orderId.asText()
                );
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private boolean isValidSignature(
            String payload,
            String signature
    ) {
        if (
                signature == null ||
                signature.isBlank()
        ) {
            return false;
        }

        String expected =
                hmacSha512(payload);

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hmacSha512(
            String payload
    ) {
        try {
            Mac mac =
                    Mac.getInstance("HmacSHA512");

            mac.init(
                    new SecretKeySpec(
                            properties.secretKey()
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    ),
                            "HmacSHA512"
                    )
            );

            byte[] bytes =
                    mac.doFinal(
                            payload.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hex =
                    new StringBuilder(
                            bytes.length * 2
                    );

            for (byte value : bytes) {
                hex.append(
                        String.format(
                                "%02x",
                                value
                        )
                );
            }

            return hex.toString();

        } catch (
                NoSuchAlgorithmException |
                InvalidKeyException exception
        ) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to validate Paystack webhook signature",
                    exception
            );
        }
    }

    private JsonNode parsePayload(
            String payload
    ) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Paystack webhook payload",
                    exception
            );
        }
    }

    private JsonNode requireData(
            PaystackClient.PaystackApiResponse response
    ) {
        if (
                response.data() == null ||
                response.data().isMissingNode() ||
                response.data().isNull()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Paystack response did not include transaction data"
            );
        }

        return response.data();
    }

    private void requirePaystackKeys() {
        requirePaystackSecretKey();

        if (!properties.hasPublicKey()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Paystack public key has not been configured. Add PAYSTACK_PUBLIC_KEY to the environment."
            );
        }
    }

    private void requirePaystackSecretKey() {
        if (!properties.hasSecretKey()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Paystack secret key has not been configured. Add PAYSTACK_SECRET_KEY to the environment."
            );
        }
    }

    private String text(
            JsonNode node,
            String field
    ) {
        JsonNode value =
                node.path(field);

        if (
                value.isMissingNode() ||
                value.isNull()
        ) {
            return null;
        }

        String text =
                value.asText();

        return text == null || text.isBlank()
                ? null
                : text.trim();
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
}
