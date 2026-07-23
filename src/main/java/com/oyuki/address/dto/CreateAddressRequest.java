package com.oyuki.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(

        @NotBlank(message = "Address label is required")
        @Size(
                max = 50,
                message = "Address label cannot exceed 50 characters"
        )
        String label,

        @NotBlank(message = "Recipient name is required")
        @Size(
                max = 150,
                message = "Recipient name cannot exceed 150 characters"
        )
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
        @Size(
                max = 100,
                message = "State cannot exceed 100 characters"
        )
        String state,

        @NotBlank(message = "City is required")
        @Size(
                max = 100,
                message = "City cannot exceed 100 characters"
        )
        String city,

        @NotBlank(message = "Area is required")
        @Size(
                max = 150,
                message = "Area cannot exceed 150 characters"
        )
        String area,

        @NotBlank(message = "Street address is required")
        @Size(
                max = 500,
                message = "Street address cannot exceed 500 characters"
        )
        String streetAddress,

        @Size(
                max = 255,
                message = "Landmark cannot exceed 255 characters"
        )
        String landmark,

        @Size(
                max = 30,
                message = "Postal code cannot exceed 30 characters"
        )
        String postalCode,

        @Size(
                max = 1000,
                message = "Delivery instructions cannot exceed 1000 characters"
        )
        String deliveryInstructions,

        Boolean defaultAddress

) {
}