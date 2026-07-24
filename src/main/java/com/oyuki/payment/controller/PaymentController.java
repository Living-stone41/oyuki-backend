package com.oyuki.payment.controller;

import com.oyuki.payment.dto.InitializePaystackPaymentRequest;
import com.oyuki.payment.dto.PaystackConfigResponse;
import com.oyuki.payment.dto.PaystackInitializeResponse;
import com.oyuki.payment.dto.PaystackVerificationResponse;
import com.oyuki.payment.dto.PaymentProofResponse;
import com.oyuki.payment.dto.PlatformBankAccountResponse;
import com.oyuki.payment.dto.VerifyPaystackPaymentRequest;
import com.oyuki.payment.service.PaystackPaymentService;
import com.oyuki.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaystackPaymentService paystackPaymentService;

    public PaymentController(
            PaymentService paymentService,
            PaystackPaymentService paystackPaymentService
    ) {
        this.paymentService =
                paymentService;

        this.paystackPaymentService =
                paystackPaymentService;
    }

    /*
     * Display Oyuki bank account during checkout.
     */
    @GetMapping("/bank-account")
    public ResponseEntity<PlatformBankAccountResponse>
    getBankAccount() {
        return ResponseEntity.ok(
                paymentService
                        .getActiveBankAccount()
        );
    }

    @GetMapping("/paystack/config")
    public ResponseEntity<PaystackConfigResponse>
    getPaystackConfig() {
        return ResponseEntity.ok(
                paystackPaymentService.getConfig()
        );
    }

    @PostMapping("/orders/{orderId}/paystack/initialize")
    public ResponseEntity<PaystackInitializeResponse>
    initializePaystackPayment(
            Authentication authentication,
            @PathVariable Long orderId,

            @RequestBody(required = false)
            InitializePaystackPaymentRequest request
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paystackPaymentService.initializePayment(
                        customerId,
                        orderId,
                        request
                )
        );
    }

    @PostMapping("/orders/{orderId}/paystack/verify")
    public ResponseEntity<PaystackVerificationResponse>
    verifyPaystackPayment(
            Authentication authentication,
            @PathVariable Long orderId,

            @Valid @RequestBody
            VerifyPaystackPaymentRequest request
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paystackPaymentService.verifyPayment(
                        customerId,
                        orderId,
                        request
                )
        );
    }

    @PostMapping(
            value = "/paystack/webhook",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> handlePaystackWebhook(
            @RequestHeader(
                    value = "x-paystack-signature",
                    required = false
            )
            String signature,

            @RequestBody
            String payload
    ) {
        paystackPaymentService.handleWebhook(
                payload,
                signature
        );

        return ResponseEntity.noContent().build();
    }

    /*
     * Customer uploads transfer receipt.
     */
    @PostMapping(
            value = "/orders/{orderId}/proof",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PaymentProofResponse>
    uploadPaymentProof(
            Authentication authentication,
            @PathVariable Long orderId,

            @RequestParam BigDecimal amount,

            @RequestParam
            String senderBankName,

            @RequestParam
            String senderAccountName,

            @RequestParam
            String transactionReference,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime paymentDate,

            @RequestPart
            MultipartFile receiptFile
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.uploadPaymentProof(
                        customerId,
                        orderId,
                        amount,
                        senderBankName,
                        senderAccountName,
                        transactionReference,
                        paymentDate,
                        receiptFile
                )
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentProofResponse>>
    getMyPaymentProofs(
            Authentication authentication
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.getMyPaymentProofs(
                        customerId
                )
        );
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<PaymentProofResponse>>
    getMyOrderPaymentProofs(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService
                        .getMyOrderPaymentProofs(
                                customerId,
                                orderId
                        )
        );
    }

    @GetMapping("/proofs/{paymentProofId}/receipt")
    public ResponseEntity<Resource>
    downloadMyReceipt(
            Authentication authentication,
            @PathVariable Long paymentProofId
    ) {
        Long customerId =
                getUserId(authentication);

        PaymentService.ReceiptDownload download =
                paymentService.getCustomerReceipt(
                        customerId,
                        paymentProofId
                );

        return buildReceiptResponse(download);
    }

    private ResponseEntity<Resource>
    buildReceiptResponse(
            PaymentService.ReceiptDownload download
    ) {
        String contentType =
                download.contentType() == null
                        ? MediaType
                                .APPLICATION_OCTET_STREAM_VALUE
                        : download.contentType();

        String fileName =
                download.fileName() == null
                        ? "payment-receipt"
                        : download.fileName()
                                .replace("\"", "");

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                contentType
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + fileName
                                + "\""
                )
                .body(download.resource());
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}
