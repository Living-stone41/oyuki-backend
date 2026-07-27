package com.oyuki.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.math.BigDecimal;

public record UpdateAddressRequest(

        @NotBlank(message = "Address label is required")
        @Size(max = 50)
        String label,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 150)
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z .'\\-]{1,149}$",
                message = "Recipient name can contain letters, spaces, apostrophes and hyphens only"
        )
        String recipientName,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{7,25}$",
                message = "Enter a valid phone number"
        )
        String phone,

        @NotBlank(message = "State is required")
        @Size(max = 100)
        String state,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "Area is required")
        @Size(max = 150)
        String area,

        @NotBlank(message = "Street address is required")
        @Size(max = 500)
        String streetAddress,

        @Size(max = 255)
        String landmark,

        @Size(max = 30)
        String postalCode,

        @Size(max = 1000)
        String deliveryInstructions,

        /*
         * null means keep the current default setting.
         */
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,

        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,

        Boolean defaultAddress

) {
}   