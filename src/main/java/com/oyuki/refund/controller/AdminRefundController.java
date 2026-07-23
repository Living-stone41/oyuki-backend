package com.oyuki.refund.controller;

import com.oyuki.refund.dto.CancelRefundRequest;
import com.oyuki.refund.dto.CompleteRefundRequest;
import com.oyuki.refund.dto.CreateRefundRequest;
import com.oyuki.refund.dto.FailRefundRequest;
import com.oyuki.refund.dto.RefundResponse;
import com.oyuki.refund.enums.RefundStatus;
import com.oyuki.refund.service.OrderRefundService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {

    private final OrderRefundService refundService;

    public AdminRefundController(
            OrderRefundService refundService
    ) {
        this.refundService = refundService;
    }

    /*
     * Create a refund for a paid order.
     */
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<RefundResponse>
    createRefund(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody
            CreateRefundRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                refundService.createRefund(
                        adminId,
                        orderId,
                        request
                )
        );
    }

    /*
     * View every refund or filter by status.
     */
    @GetMapping
    public ResponseEntity<List<RefundResponse>>
    getAllRefunds(
            Authentication authentication,

            @RequestParam(required = false)
            RefundStatus status
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                refundService.getAllRefunds(
                        adminId,
                        status
                )
        );
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponse>
    getRefund(
            Authentication authentication,
            @PathVariable Long refundId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                refundService.getRefund(
                        adminId,
                        refundId
                )
        );
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<RefundResponse>>
    getOrderRefunds(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                refundService.getOrderRefunds(
                        adminId,
                        orderId
                )
        );
    }

    @PatchMapping("/{refundId}/processing")
    public ResponseEntity<RefundResponse>
    startProcessing(
            Authentication authentication,
            @PathVariable Long refundId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                refundService.startProcessing(
                        adminId,
                        refundId
                )
        );
    }

    @PatchMapping("/{refundId}/complete")
    public ResponseEntity<RefundResponse>
    completeRefund(
            Authentication authentication,
            @PathVariable Long refundId,
            @Valid @RequestBody
            CompleteRefundRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                refundService.completeRefund(
                        adminId,
                        refundId,
                        request
                )
        );
    }

    @PatchMapping("/{refundId}/fail")
    public ResponseEntity<RefundResponse>
    failRefund(
            Authentication authentication,
            @PathVariable Long refundId,
            @Valid @RequestBody
            FailRefundRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                refundService.failRefund(
                        adminId,
                        refundId,
                        request
                )
        );
    }

    @PatchMapping("/{refundId}/cancel")
    public ResponseEntity<RefundResponse>
    cancelRefund(
            Authentication authentication,
            @PathVariable Long refundId,
            @Valid @RequestBody
            CancelRefundRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                refundService.cancelRefund(
                        adminId,
                        refundId,
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