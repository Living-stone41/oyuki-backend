package com.oyuki.coupon.dto;

import com.oyuki.coupon.enums.DiscountType;

import java.math.BigDecimal;

public record CouponValidationResponse(

        boolean valid,
        String message,

        Long couponId,
        String code,
        DiscountType discountType,

        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discountAmount,
        BigDecimal totalAfterDiscount,

        Integer remainingGlobalUses,
        Integer remainingCustomerUses

) {
}
