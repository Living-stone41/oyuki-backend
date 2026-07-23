package com.oyuki.providerlocation.dto;

import com.oyuki.delivery.enums.NigeriaState;
import com.oyuki.providerlocation.entity.ProviderPickupAddress;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProviderPickupAddressResponse(

        Long id,

        Long providerId,
        String providerName,
        String providerRole,

        String country,

        NigeriaState state,
        String stateName,

        String city,
        String lga,
        String area,
        String streetAddress,
        String landmark,

        BigDecimal latitude,
        BigDecimal longitude,

        boolean active,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static ProviderPickupAddressResponse from(
            ProviderPickupAddress address
    ) {
        return new ProviderPickupAddressResponse(
                address.getId(),

                address.getProvider().getId(),
                address.getProvider().getFullName(),
                address.getProvider().getRole().name(),

                address.getCountry(),

                address.getState(),
                address.getState().getDisplayName(),

                address.getCity(),
                address.getLga(),
                address.getArea(),
                address.getStreetAddress(),
                address.getLandmark(),

                address.getLatitude(),
                address.getLongitude(),

                address.isActive(),

                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
