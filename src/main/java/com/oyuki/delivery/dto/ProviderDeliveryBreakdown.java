package com.oyuki.delivery.dto;

import com.oyuki.delivery.enums.DeliveryRateType;
import com.oyuki.delivery.enums.NigeriaState;

import java.math.BigDecimal;

public record ProviderDeliveryBreakdown(

        Long providerId,
        String providerName,

        NigeriaState originState,
        String originCity,
        String originArea,

        NigeriaState destinationState,
        String destinationCity,
        String destinationArea,

        DeliveryRateType appliedRateType,

        BigDecimal pickupFee,

        String message

) {
}
