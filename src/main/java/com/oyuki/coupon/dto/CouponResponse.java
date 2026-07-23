package com.oyuki.coupon.dto;

import com.oyuki.coupon.entity.Coupon;
import com.oyuki.coupon.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(

        Long id,
        String code,
        String description,

        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maximumDiscountAmount,
        BigDecimal minimumOrderAmount,

        Integer usageLimit,
        Integer perCustomerLimit,

        long totalUsageCount,

        LocalDateTime startsAt,
        LocalDateTime expiresAt,

        boolean active,

        Long createdById,
        String createdByName,

        Long updatedById,
        String updatedByName,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static CouponResponse from(
            Coupon coupon,
            long totalUsageCount
    ) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),

                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMaximumDiscountAmount(),
                coupon.getMinimumOrderAmount(),

                coupon.getUsageLimit(),
                coupon.getPerCustomerLimit(),

                totalUsageCount,

                coupon.getStartsAt(),
                coupon.getExpiresAt(),

                coupon.isActive(),

                coupon.getCreatedBy() == null
                        ? null
                        : coupon.getCreatedBy().getId(),

                coupon.getCreatedBy() == null
                        ? null
                        : coupon.getCreatedBy().getFullName(),

                coupon.getUpdatedBy() == null
                        ? null
                        : coupon.getUpdatedBy().getId(),

                coupon.getUpdatedBy() == null
                        ? null
                        : coupon.getUpdatedBy().getFullName(),

                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }
}
