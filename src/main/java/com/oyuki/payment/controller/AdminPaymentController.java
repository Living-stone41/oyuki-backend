package com.oyuki.payment.controller;

import com.oyuki.payment.dto.BankAccountRequest;
import com.oyuki.payment.dto.ConfirmPaymentProofRequest;
import com.oyuki.payment.dto.PaymentProofResponse;
import com.oyuki.payment.dto.PlatformBankAccountResponse;
import com.oyuki.payment.dto.RejectPaymentProofRequest;
import com.oyuki.payment.enums.PaymentProofStatus;
import com.oyuki.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(
            PaymentService paymentService
    ) {
        this.paymentService =
                paymentService;
    }

    /*
     * Create or replace active Oyuki bank account.
     */
    @PostMapping("/bank-account")
    public ResponseEntity<PlatformBankAccountResponse>
    createBankAccount(
            Authentication authentication,
            @Valid @RequestBody
            BankAccountRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.createBankAccount(
                        adminId,
                        request
                )
        );
    }

    @GetMapping("/bank-accounts")
    public ResponseEntity<
            List<PlatformBankAccountResponse>
            >
    getAllBankAccounts(
            Authentication authentication
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.getAllBankAccounts(
                        adminId
                )
        );
    }

    @PatchMapping(
            "/bank-accounts/{bankAccountId}/activate"
    )
    public ResponseEntity<PlatformBankAccountResponse>
    activateBankAccount(
            Authentication authentication,
            @PathVariable Long bankAccountId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.activateBankAccount(
                        adminId,
                        bankAccountId
                )
        );
    }

    /*
     * View all uploaded payment receipts.
     */
    @GetMapping
    public ResponseEntity<List<PaymentProofResponse>>
    getPaymentProofs(
            Authentication authentication,

            @RequestParam(required = false)
            PaymentProofStatus status
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.getAllPaymentProofs(
                        adminId,
                        status
                )
        );
    }

    @GetMapping("/{paymentProofId}")
    public ResponseEntity<PaymentProofResponse>
    getPaymentProof(
            Authentication authentication,
            @PathVariable Long paymentProofId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.getAdminPaymentProof(
                        adminId,
                        paymentProofId
                )
        );
    }

    @GetMapping("/{paymentProofId}/receipt")
    public ResponseEntity<Resource>
    downloadReceipt(
            Authentication authentication,
            @PathVariable Long paymentProofId
    ) {
        Long adminId =
                getUserId(authentication);

        PaymentService.ReceiptDownload download =
                paymentService.getAdminReceipt(
                        adminId,
                        paymentProofId
                );

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

    @PatchMapping(
            "/{paymentProofId}/confirm"
    )
    public ResponseEntity<PaymentProofResponse>
    confirmPayment(
            Authentication authentication,
            @PathVariable Long paymentProofId,
            @Valid @RequestBody
            ConfirmPaymentProofRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.confirmPaymentProof(
                        adminId,
                        paymentProofId,
                        request
                )
        );
    }

    @PatchMapping(
            "/{paymentProofId}/reject"
    )
    public ResponseEntity<PaymentProofResponse>
    rejectPayment(
            Authentication authentication,
            @PathVariable Long paymentProofId,
            @Valid @RequestBody
            RejectPaymentProofRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                paymentService.rejectPaymentProof(
                        adminId,
                        paymentProofId,
                        request
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}