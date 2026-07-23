package com.oyuki.delivery.dto;

import com.oyuki.delivery.enums.NigeriaState;

import java.math.BigDecimal;
import java.util.List;

public record DeliveryFeeResponse(

        Long addressId,

        NigeriaState destinationState,
        String destinationCity,
        String destinationArea,

        int providerCount,
        int uniquePickupLocationCount,

        BigDecimal providerPickupFees,
        BigDecimal additionalProviderFees,
        BigDecimal finalDeliveryFee,
        BigDecimal totalDeliveryFee,

        Integer estimatedMinDays,
        Integer estimatedMaxDays,

        List<ProviderDeliveryBreakdown> providerBreakdown

) {
}
