package com.oyuki.providerlocation.dto;

import com.oyuki.delivery.enums.NigeriaState;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SaveProviderPickupAddressRequest(

        @NotNull(message = "State is required")
        NigeriaState state,

        @NotBlank(message = "City is required")
        @Size(max = 120)
        String city,

        @NotBlank(message = "LGA is required")
        @Size(max = 120)
        String lga,

        @NotBlank(message = "Area is required")
        @Size(max = 160)
        String area,

        @NotBlank(message = "Street address is required")
        @Size(max = 500)
        String streetAddress,

        @Size(max = 255)
        String landmark,

        @DecimalMin(value = "-90.0000000")
        @DecimalMax(value = "90.0000000")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0000000")
        @DecimalMax(value = "180.0000000")
        BigDecimal longitude,

        Boolean active

) {
}
