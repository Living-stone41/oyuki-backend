package com.oyuki.coupon.controller;

import com.oyuki.coupon.dto.*;
import com.oyuki.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(
            CouponService couponService
    ) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(
            Authentication authentication,
            @Valid @RequestBody
            CreateCouponRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        couponService.createCoupon(
                                getUserId(authentication),
                                request
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getCoupons(
            Authentication authentication,
            @RequestParam(required = false)
            Boolean active
    ) {
        return ResponseEntity.ok(
                couponService.getCoupons(
                        getUserId(authentication),
                        active
                )
        );
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse> getCoupon(
            Authentication authentication,
            @PathVariable Long couponId
    ) {
        return ResponseEntity.ok(
                couponService.getCoupon(
                        getUserId(authentication),
                        couponId
                )
        );
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<CouponResponse> updateCoupon(
            Authentication authentication,
            @PathVariable Long couponId,
            @Valid @RequestBody
            UpdateCouponRequest request
    ) {
        return ResponseEntity.ok(
                couponService.updateCoupon(
                        getUserId(authentication),
                        couponId,
                        request
                )
        );
    }

    @PatchMapping("/{couponId}/status")
    public ResponseEntity<CouponResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long couponId,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(
                couponService.updateStatus(
                        getUserId(authentication),
                        couponId,
                        active
                )
        );
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(
            Authentication authentication,
            @PathVariable Long couponId
    ) {
        couponService.deleteCoupon(
                getUserId(authentication),
                couponId
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
