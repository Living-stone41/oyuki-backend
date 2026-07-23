package com.oyuki.coupon.dto;

import com.oyuki.coupon.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponRequest(

        @NotBlank(message = "Coupon code is required")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]{3,40}$",
                message = "Coupon code can only contain letters, numbers, underscores and hyphens"
        )
        String code,

        @Size(max = 500)
        String description,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Discount value cannot be negative"
        )
        BigDecimal discountValue,

        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Maximum discount cannot be negative"
        )
        BigDecimal maximumDiscountAmount,

        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Minimum order amount cannot be negative"
        )
        BigDecimal minimumOrderAmount,

        @Min(value = 1, message = "Usage limit must be at least 1")
        Integer usageLimit,

        @Min(value = 1, message = "Per-customer limit must be at least 1")
        Integer perCustomerLimit,

        LocalDateTime startsAt,

        LocalDateTime expiresAt,

        Boolean active

) {
}
