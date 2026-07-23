package com.oyuki.kitchen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record KitchenProfileRequest(

        @NotBlank(message = "Kitchen name is required")
        @Size(max = 150)
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9 .&'()\\-]{1,149}$",
                message = "Kitchen name contains unsupported characters"
        )
        String kitchenName,

        @NotBlank(message = "Kitchen bio is required")
        @Size(min = 20, max = 1500)
        String bio,

        @NotBlank(message = "Cuisine information is required")
        @Size(max = 250)
        String cuisine,

        @Size(max = 500)
        String profileImageUrl,

        @Size(max = 500)
        String coverImageUrl,

        @NotBlank(message = "State is required")
        @Size(max = 100)
        String state,

        @NotBlank(message = "LGA is required")
        @Size(max = 100)
        String lga,

        @NotBlank(message = "Area is required")
        @Size(max = 150)
        String area,

        @NotBlank(message = "Full address is required")
        @Size(max = 500)
        String addressLine,

        BigDecimal latitude,

        BigDecimal longitude,

        @Size(max = 500)
        String idDocumentUrl,

        @Pattern(
                regexp = "^$|^[A-Za-z][A-Za-z .&'\\-]{1,149}$",
                message = "Bank name can contain letters, spaces, apostrophes, hyphens and & only"
        )
        String bankName,

        @Pattern(
                regexp = "^$|^[A-Za-z][A-Za-z .'\\-]{1,149}$",
                message = "Account name can contain letters, spaces, apostrophes and hyphens only"
        )
        String accountName,

        @Pattern(
                regexp = "^$|^[0-9]{10}$",
                message = "Account number must contain exactly 10 digits"
        )
        String accountNumber
) {
}
