package com.oyuki.delivery.dto;

import com.oyuki.delivery.entity.DeliveryRate;
import com.oyuki.delivery.enums.DeliveryRateType;
import com.oyuki.delivery.enums.NigeriaState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryRateResponse(

        Long id,
        DeliveryRateType rateType,

        NigeriaState originState,
        String originStateName,

        NigeriaState destinationState,
        String destinationStateName,

        String originCity,
        String destinationCity,
        String originArea,
        String destinationArea,

        BigDecimal fee,

        Integer estimatedMinDays,
        Integer estimatedMaxDays,

        boolean active,

        Long createdById,
        String createdByName,

        Long updatedById,
        String updatedByName,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static DeliveryRateResponse from(
            DeliveryRate rate
    ) {
        return new DeliveryRateResponse(
                rate.getId(),
                rate.getRateType(),

                rate.getOriginState(),
                rate.getOriginState() == null
                        ? null
                        : rate.getOriginState().getDisplayName(),

                rate.getDestinationState(),
                rate.getDestinationState() == null
                        ? null
                        : rate.getDestinationState().getDisplayName(),

                rate.getOriginCity(),
                rate.getDestinationCity(),
                rate.getOriginArea(),
                rate.getDestinationArea(),

                rate.getFee(),

                rate.getEstimatedMinDays(),
                rate.getEstimatedMaxDays(),

                rate.isActive(),

                rate.getCreatedBy() == null
                        ? null
                        : rate.getCreatedBy().getId(),

                rate.getCreatedBy() == null
                        ? null
                        : rate.getCreatedBy().getFullName(),

                rate.getUpdatedBy() == null
                        ? null
                        : rate.getUpdatedBy().getId(),

                rate.getUpdatedBy() == null
                        ? null
                        : rate.getUpdatedBy().getFullName(),

                rate.getCreatedAt(),
                rate.getUpdatedAt()
        );
    }
}
