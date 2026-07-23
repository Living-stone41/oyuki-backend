package com.oyuki.delivery.dto;

import com.oyuki.delivery.enums.DeliveryRateType;
import com.oyuki.delivery.enums.NigeriaState;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateDeliveryRateRequest(

        @NotNull(message = "Delivery rate type is required")
        DeliveryRateType rateType,

        NigeriaState originState,

        NigeriaState destinationState,

        @Size(max = 120)
        String originCity,

        @Size(max = 120)
        String destinationCity,

        @Size(max = 160)
        String originArea,

        @Size(max = 160)
        String destinationArea,

        @NotNull(message = "Delivery fee is required")
        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "Delivery fee cannot be negative"
        )
        BigDecimal fee,

        @Min(value = 0, message = "Minimum delivery days cannot be negative")
        Integer estimatedMinDays,

        @Min(value = 0, message = "Maximum delivery days cannot be negative")
        Integer estimatedMaxDays,

        Boolean active

) {
}
