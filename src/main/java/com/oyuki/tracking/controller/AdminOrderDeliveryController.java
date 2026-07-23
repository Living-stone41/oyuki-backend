package com.oyuki.tracking.controller;

import com.oyuki.tracking.dto.AssignOrderRiderRequest;
import com.oyuki.tracking.dto.OrderDeliveryResponse;
import com.oyuki.tracking.enums.OrderDeliveryStatus;
import com.oyuki.tracking.service.OrderDeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/order-deliveries")
public class AdminOrderDeliveryController {

    private final OrderDeliveryService orderDeliveryService;

    public AdminOrderDeliveryController(
            OrderDeliveryService orderDeliveryService
    ) {
        this.orderDeliveryService =
                orderDeliveryService;
    }

    @PostMapping("/orders/{orderId}/assign-rider")
    public ResponseEntity<OrderDeliveryResponse>
    assignRider(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody
            AssignOrderRiderRequest request
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService.assignRider(
                        adminId,
                        orderId,
                        request
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<OrderDeliveryResponse>>
    getDeliveries(
            Authentication authentication,

            @RequestParam(required = false)
            OrderDeliveryStatus status
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .getAdminDeliveries(
                                adminId,
                                status
                        )
        );
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<OrderDeliveryResponse>
    getDelivery(
            Authentication authentication,
            @PathVariable Long deliveryId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .getAdminDelivery(
                                adminId,
                                deliveryId
                        )
        );
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDeliveryResponse>
    getOrderDelivery(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .getAdminOrderDelivery(
                                adminId,
                                orderId
                        )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}