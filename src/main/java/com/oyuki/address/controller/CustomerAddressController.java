package com.oyuki.address.controller;

import com.oyuki.address.dto.AddressResponse;
import com.oyuki.address.dto.CreateAddressRequest;
import com.oyuki.address.dto.UpdateAddressRequest;
import com.oyuki.address.service.CustomerAddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    public CustomerAddressController(
            CustomerAddressService addressService
    ) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<AddressResponse>
    createAddress(
            Authentication authentication,
            @Valid @RequestBody
            CreateAddressRequest request
    ) {
        AddressResponse response =
                addressService.createAddress(
                        getUserId(authentication),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>>
    getAddresses(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                addressService.getAddresses(
                        getUserId(authentication)
                )
        );
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse>
    getAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        return ResponseEntity.ok(
                addressService.getAddress(
                        getUserId(authentication),
                        addressId
                )
        );
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse>
    updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @Valid @RequestBody
            UpdateAddressRequest request
    ) {
        return ResponseEntity.ok(
                addressService.updateAddress(
                        getUserId(authentication),
                        addressId,
                        request
                )
        );
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<AddressResponse>
    setDefaultAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        return ResponseEntity.ok(
                addressService.setDefaultAddress(
                        getUserId(authentication),
                        addressId
                )
        );
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void>
    deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        addressService.deleteAddress(
                getUserId(authentication),
                addressId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
}