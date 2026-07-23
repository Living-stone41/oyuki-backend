package com.oyuki.logistics.controller;

import com.oyuki.logistics.dto.AssignRiderRequest;
import com.oyuki.logistics.dto.DeliveryResponse;
import com.oyuki.logistics.enums.DeliveryStatus;
import com.oyuki.logistics.service.LogisticsService;
import com.oyuki.order.dto.OrderItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    private final LogisticsService logisticsService;

    public LogisticsController(
            LogisticsService logisticsService
    ) {
        this.logisticsService = logisticsService;
    }

    /*
     * VIEW ITEMS WAITING FOR RIDER ASSIGNMENT.
     */
    @GetMapping("/ready-items")
    public ResponseEntity<List<OrderItemResponse>>
    getReadyItems(
            Authentication authentication
    ) {
        Long logisticsAdminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                logisticsService.getReadyItems(
                        logisticsAdminId
                )
        );
    }

    /*
     * VIEW ALL DELIVERIES OR FILTER BY STATUS.
     */
    @GetMapping("/deliveries")
    public ResponseEntity<List<DeliveryResponse>>
    getDeliveries(
            Authentication authentication,

            @RequestParam(
                    required = false
            ) DeliveryStatus status
    ) {
        Long logisticsAdminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                logisticsService.getDeliveries(
                        logisticsAdminId,
                        status
                )
        );
    }

    /*
     * ASSIGN RIDER TO READY ORDER ITEM.
     */
    @PostMapping(
            "/ready-items/{orderItemId}/assign"
    )
    public ResponseEntity<DeliveryResponse>
    assignRider(
            Authentication authentication,
            @PathVariable Long orderItemId,
            @Valid @RequestBody
            AssignRiderRequest request
    ) {
        Long logisticsAdminId =
                getUserId(authentication);

        DeliveryResponse response =
                logisticsService.assignRider(
                        logisticsAdminId,
                        orderItemId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}