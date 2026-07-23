package com.oyuki.tracking.controller;

import com.oyuki.tracking.dto.OrderDeliveryResponse;
import com.oyuki.tracking.service.OrderDeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
public class OrderDeliveryController {

    private final OrderDeliveryService orderDeliveryService;

    public OrderDeliveryController(
            OrderDeliveryService orderDeliveryService
    ) {
        this.orderDeliveryService =
                orderDeliveryService;
    }

    @GetMapping("/my-deliveries")
    public ResponseEntity<List<OrderDeliveryResponse>>
    getMyDeliveries(
            Authentication authentication
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .getCustomerDeliveries(
                                customerId
                        )
        );
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDeliveryResponse>
    getOrderDelivery(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .getCustomerOrderDelivery(
                                customerId,
                                orderId
                        )
        );
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<OrderDeliveryResponse>
    trackDelivery(
            Authentication authentication,
            @PathVariable String trackingNumber
    ) {
        Long customerId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .trackByTrackingNumber(
                                customerId,
                                trackingNumber
                        )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}