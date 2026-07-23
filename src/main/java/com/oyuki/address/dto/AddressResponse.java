package com.oyuki.address.dto;

import com.oyuki.address.entity.CustomerAddress;

import java.time.LocalDateTime;

public record AddressResponse(

        Long id,

        String label,
        String recipientName,
        String phone,

        String state,
        String city,
        String area,
        String streetAddress,
        String landmark,
        String postalCode,
        String deliveryInstructions,

        boolean defaultAddress,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static AddressResponse from(
            CustomerAddress address
    ) {
        return new AddressResponse(
                address.getId(),

                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),

                address.getState(),
                address.getCity(),
                address.getArea(),
                address.getStreetAddress(),
                address.getLandmark(),
                address.getPostalCode(),
                address.getDeliveryInstructions(),

                address.isDefaultAddress(),

                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}