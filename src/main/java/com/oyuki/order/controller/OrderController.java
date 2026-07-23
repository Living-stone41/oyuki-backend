package com.oyuki.order.controller;

import com.oyuki.order.dto.CheckoutRequest;
import com.oyuki.order.dto.OrderItemResponse;
import com.oyuki.order.dto.OrderResponse;
import com.oyuki.order.dto.RejectOrderRequest;
import com.oyuki.order.enums.OrderItemStatus;
import com.oyuki.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    /*
     * CUSTOMER CHECKOUT
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request
    ) {
        Long customerId =
                getUserId(authentication);

        OrderResponse response =
                orderService.checkout(
                        customerId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * CUSTOMER ORDER HISTORY
     */
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>>
    getMyOrders(
            Authentication authentication
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderService.getMyOrders(
                        customerId
                )
        );
    }

    /*
     * CUSTOMER VIEWS ONE ORDER
     */
    @GetMapping("/my/{orderId}")
    public ResponseEntity<OrderResponse>
    getMyOrder(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderService.getMyOrder(
                        customerId,
                        orderId
                )
        );
    }

    /*
     * SELLER OR KITCHEN VIEWS THEIR ORDER ITEMS.
     *
     * All items:
     * GET /api/orders/provider/items
     *
     * Filter by status:
     * GET /api/orders/provider/items?status=PENDING
     */
    @GetMapping("/provider/items")
    public ResponseEntity<List<OrderItemResponse>>
    getProviderOrderItems(
            Authentication authentication,

            @RequestParam(
                    required = false
            )
            OrderItemStatus status
    ) {
        Long providerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderService.getProviderItems(
                        providerId,
                        status
                )
        );
    }

    /*
     * PROVIDER ACCEPTS ITEM
     */
    @PatchMapping(
            "/provider/items/{orderItemId}/accept"
    )
    public ResponseEntity<OrderItemResponse>
    acceptOrderItem(
            Authentication authentication,
            @PathVariable Long orderItemId
    ) {
        Long providerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderService.acceptOrderItem(
                        providerId,
                        orderItemId
                )
        );
    }

    /*
     * PROVIDER REJECTS ITEM
     */
    @PatchMapping(
            "/provider/items/{orderItemId}/reject"
    )
    public ResponseEntity<OrderItemResponse>
    rejectOrderItem(
            Authentication authentication,
            @PathVariable Long orderItemId,
            @Valid @RequestBody
            RejectOrderRequest request
    ) {
        Long providerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderService.rejectOrderItem(
                        providerId,
                        orderItemId,
                        request
                )
        );
    }

    /*
     * PROVIDER STARTS PROCESSING ITEM
     */
    @PatchMapping(
            "/provider/items/{orderItemId}/processing"
    )
    public ResponseEntity<OrderItemResponse>
    startProcessing(
            Authentication authentication,
            @PathVariable Long orderItemId
    ) {
        Long providerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderService.markOrderItemProcessing(
                        providerId,
                        orderItemId
                )
        );
    }

    /*
     * PROVIDER MARKS ITEM READY FOR PICKUP
     */
    @PatchMapping(
            "/provider/items/{orderItemId}/ready"
    )
    public ResponseEntity<OrderItemResponse>
    markReady(
            Authentication authentication,
            @PathVariable Long orderItemId
    ) {
        Long providerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderService.markOrderItemReady(
                        providerId,
                        orderItemId
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}