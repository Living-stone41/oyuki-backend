package com.oyuki.providerlocation.controller;

import com.oyuki.providerlocation.dto.ProviderPickupAddressResponse;
import com.oyuki.providerlocation.dto.SaveProviderPickupAddressRequest;
import com.oyuki.providerlocation.service.ProviderPickupAddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/provider/pickup-address")
public class ProviderPickupAddressController {

    private final ProviderPickupAddressService pickupAddressService;

    public ProviderPickupAddressController(
            ProviderPickupAddressService pickupAddressService
    ) {
        this.pickupAddressService = pickupAddressService;
    }

    @PutMapping
    public ResponseEntity<ProviderPickupAddressResponse> saveAddress(
            Authentication authentication,
            @Valid @RequestBody
            SaveProviderPickupAddressRequest request
    ) {
        return ResponseEntity.ok(
                pickupAddressService.saveAddress(
                        getUserId(authentication),
                        request
                )
        );
    }

    @GetMapping
    public ResponseEntity<ProviderPickupAddressResponse> getMyAddress(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                pickupAddressService.getMyAddress(
                        getUserId(authentication)
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}
