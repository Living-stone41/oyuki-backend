package com.oyuki.delivery.controller;

import com.oyuki.delivery.dto.CreateDeliveryRateRequest;
import com.oyuki.delivery.dto.DeliveryRateResponse;
import com.oyuki.delivery.dto.UpdateDeliveryRateRequest;
import com.oyuki.delivery.enums.DeliveryRateType;
import com.oyuki.delivery.service.DeliveryRateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/delivery-rates")
public class AdminDeliveryRateController {

    private final DeliveryRateService deliveryRateService;

    public AdminDeliveryRateController(
            DeliveryRateService deliveryRateService
    ) {
        this.deliveryRateService = deliveryRateService;
    }

    @PostMapping
    public ResponseEntity<DeliveryRateResponse> createRate(
            Authentication authentication,
            @Valid @RequestBody
            CreateDeliveryRateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        deliveryRateService.createRate(
                                getUserId(authentication),
                                request
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<DeliveryRateResponse>> getRates(
            Authentication authentication,
            @RequestParam(required = false)
            DeliveryRateType rateType,
            @RequestParam(required = false)
            Boolean active
    ) {
        return ResponseEntity.ok(
                deliveryRateService.getRates(
                        getUserId(authentication),
                        rateType,
                        active
                )
        );
    }

    @GetMapping("/{rateId}")
    public ResponseEntity<DeliveryRateResponse> getRate(
            Authentication authentication,
            @PathVariable Long rateId
    ) {
        return ResponseEntity.ok(
                deliveryRateService.getRate(
                        getUserId(authentication),
                        rateId
                )
        );
    }

    @PutMapping("/{rateId}")
    public ResponseEntity<DeliveryRateResponse> updateRate(
            Authentication authentication,
            @PathVariable Long rateId,
            @Valid @RequestBody
            UpdateDeliveryRateRequest request
    ) {
        return ResponseEntity.ok(
                deliveryRateService.updateRate(
                        getUserId(authentication),
                        rateId,
                        request
                )
        );
    }

    @PatchMapping("/{rateId}/status")
    public ResponseEntity<DeliveryRateResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long rateId,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(
                deliveryRateService.updateStatus(
                        getUserId(authentication),
                        rateId,
                        active
                )
        );
    }

    @DeleteMapping("/{rateId}")
    public ResponseEntity<Void> deleteRate(
            Authentication authentication,
            @PathVariable Long rateId
    ) {
        deliveryRateService.deleteRate(
                getUserId(authentication),
                rateId
        );

        return ResponseEntity.noContent().build();
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}
