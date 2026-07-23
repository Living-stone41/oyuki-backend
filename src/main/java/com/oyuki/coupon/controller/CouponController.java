package com.oyuki.coupon.controller;

import com.oyuki.coupon.dto.CouponValidationResponse;
import com.oyuki.coupon.dto.ValidateCouponRequest;
import com.oyuki.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(
            CouponService couponService
    ) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            Authentication authentication,
            @Valid @RequestBody
            ValidateCouponRequest request
    ) {
        return ResponseEntity.ok(
                couponService.validateCoupon(
                        getUserId(authentication),
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
