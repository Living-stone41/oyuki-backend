package com.oyuki.tracking.controller;

import com.oyuki.tracking.dto.OrderDeliveryResponse;
import com.oyuki.tracking.dto.UpdateDeliveryNoteRequest;
import com.oyuki.tracking.enums.OrderDeliveryStatus;
import com.oyuki.tracking.service.OrderDeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rider/order-deliveries")
public class RiderOrderDeliveryController {

    private final OrderDeliveryService orderDeliveryService;

    public RiderOrderDeliveryController(
            OrderDeliveryService orderDeliveryService
    ) {
        this.orderDeliveryService =
                orderDeliveryService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDeliveryResponse>>
    getMyDeliveries(
            Authentication authentication,

            @RequestParam(required = false)
            OrderDeliveryStatus status
    ) {
        Long riderId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .getRiderDeliveries(
                                riderId,
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
        Long riderId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .getRiderDelivery(
                                riderId,
                                deliveryId
                        )
        );
    }

    @PatchMapping("/{deliveryId}/accept")
    public ResponseEntity<OrderDeliveryResponse>
    acceptDelivery(
            Authentication authentication,
            @PathVariable Long deliveryId,

            @Valid
            @RequestBody(required = false)
            UpdateDeliveryNoteRequest request
    ) {
        Long riderId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .acceptDelivery(
                                riderId,
                                deliveryId,
                                request
                        )
        );
    }

    @PatchMapping("/{deliveryId}/picked-up")
    public ResponseEntity<OrderDeliveryResponse>
    markPickedUp(
            Authentication authentication,
            @PathVariable Long deliveryId,

            @Valid
            @RequestBody(required = false)
            UpdateDeliveryNoteRequest request
    ) {
        Long riderId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .markPickedUp(
                                riderId,
                                deliveryId,
                                request
                        )
        );
    }

    @PatchMapping("/{deliveryId}/out-for-delivery")
    public ResponseEntity<OrderDeliveryResponse>
    markOutForDelivery(
            Authentication authentication,
            @PathVariable Long deliveryId,

            @Valid
            @RequestBody(required = false)
            UpdateDeliveryNoteRequest request
    ) {
        Long riderId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .markOutForDelivery(
                                riderId,
                                deliveryId,
                                request
                        )
        );
    }

    @PatchMapping("/{deliveryId}/delivered")
    public ResponseEntity<OrderDeliveryResponse>
    markDelivered(
            Authentication authentication,
            @PathVariable Long deliveryId,

            @Valid
            @RequestBody(required = false)
            UpdateDeliveryNoteRequest request
    ) {
        Long riderId =
                getUserId(authentication);

        return ResponseEntity.ok(
                orderDeliveryService
                        .markDelivered(
                                riderId,
                                deliveryId,
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