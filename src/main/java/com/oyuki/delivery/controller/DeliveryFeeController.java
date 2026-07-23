package com.oyuki.delivery.controller;

import com.oyuki.delivery.dto.DeliveryFeeRequest;
import com.oyuki.delivery.dto.DeliveryFeeResponse;
import com.oyuki.delivery.service.DeliveryFeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery-fees")
public class DeliveryFeeController {

    private final DeliveryFeeService deliveryFeeService;

    public DeliveryFeeController(
            DeliveryFeeService deliveryFeeService
    ) {
        this.deliveryFeeService = deliveryFeeService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<DeliveryFeeResponse> calculateFee(
            Authentication authentication,
            @Valid @RequestBody
            DeliveryFeeRequest request
    ) {
        return ResponseEntity.ok(
                deliveryFeeService.calculateCartDeliveryFee(
                        getUserId(authentication),
                        request.addressId()
                )
        );
    }

    @GetMapping("/calculate")
    public ResponseEntity<DeliveryFeeResponse> calculateFee(
            Authentication authentication,
            @RequestParam Long addressId
    ) {
        return ResponseEntity.ok(
                deliveryFeeService.calculateCartDeliveryFee(
                        getUserId(authentication),
                        addressId
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}
