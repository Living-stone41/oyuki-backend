package com.oyuki.logistics.controller;

import com.oyuki.logistics.dto.DeliveryResponse;
import com.oyuki.logistics.dto.DeliveryStatusRequest;
import com.oyuki.logistics.enums.DeliveryStatus;
import com.oyuki.logistics.service.RiderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rider")
public class RiderController {

    private final RiderService riderService;

    public RiderController(
            RiderService riderService
    ) {
        this.riderService = riderService;
    }

    /*
     * VIEW RIDER DELIVERIES.
     */
    @GetMapping("/deliveries")
    public ResponseEntity<List<DeliveryResponse>>
    getMyDeliveries(
            Authentication authentication,

            @RequestParam(
                    required = false
            ) DeliveryStatus status
    ) {
        Long riderId = getUserId(authentication);

        return ResponseEntity.ok(
                riderService.getMyDeliveries(
                        riderId,
                        status
                )
        );
    }

    /*
     * VIEW ONE DELIVERY.
     */
    @GetMapping("/deliveries/{deliveryId}")
    public ResponseEntity<DeliveryResponse>
    getMyDelivery(
            Authentication authentication,
            @PathVariable Long deliveryId
    ) {
        Long riderId = getUserId(authentication);

        return ResponseEntity.ok(
                riderService.getMyDelivery(
                        riderId,
                        deliveryId
                )
        );
    }

    /*
     * UPDATE DELIVERY STATUS.
     */
    @PatchMapping(
            "/deliveries/{deliveryId}/status"
    )
    public ResponseEntity<DeliveryResponse>
    updateStatus(
            Authentication authentication,
            @PathVariable Long deliveryId,
            @Valid @RequestBody
            DeliveryStatusRequest request
    ) {
        Long riderId = getUserId(authentication);

        return ResponseEntity.ok(
                riderService.updateStatus(
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