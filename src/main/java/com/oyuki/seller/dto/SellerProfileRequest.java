package com.oyuki.seller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SellerProfileRequest(

        @NotBlank(message = "Business name is required")
        @Size(
                max = 150,
                message = "Business name cannot exceed 150 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9 .&'()\\-]{1,149}$",
                message = "Business name contains unsupported characters"
        )
        String businessName,

        @NotBlank(message = "Seller bio is required")
        @Size(
                min = 20,
                max = 1500,
                message = "Seller bio must contain between 20 and 1500 characters"
        )
        String bio,

        @Size(
                max = 500,
                message = "Profile image URL cannot exceed 500 characters"
        )
        String profileImageUrl,

        @Size(
                max = 500,
                message = "Cover image URL cannot exceed 500 characters"
        )
        String coverImageUrl,

        @NotBlank(message = "State is required")
        @Size(
                max = 100,
                message = "State cannot exceed 100 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z .'-]{1,99}$",
                message = "State contains unsupported characters"
        )
        String state,

        @NotBlank(message = "LGA is required")
        @Size(
                max = 100,
                message = "LGA cannot exceed 100 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9 .'-]{1,99}$",
                message = "LGA contains unsupported characters"
        )
        String lga,

        @NotBlank(message = "Area is required")
        @Size(
                max = 150,
                message = "Area cannot exceed 150 characters"
        )
        String area,

        @NotBlank(message = "Full address is required")
        @Size(
                max = 500,
                message = "Full address cannot exceed 500 characters"
        )
        String addressLine,

        @DecimalMin(
                value = "-90.0",
                message = "Latitude must not be lower than -90"
        )
        @DecimalMax(
                value = "90.0",
                message = "Latitude must not be greater than 90"
        )
        BigDecimal latitude,

        @DecimalMin(
                value = "-180.0",
                message = "Longitude must not be lower than -180"
        )
        @DecimalMax(
                value = "180.0",
                message = "Longitude must not be greater than 180"
        )
        BigDecimal longitude,

        @Size(
                max = 500,
                message = "ID document URL cannot exceed 500 characters"
        )
        String idDocumentUrl,

        @Size(
                max = 500,
                message = "Business document URL cannot exceed 500 characters"
        )
        String businessDocumentUrl,

        @Size(
                max = 500,
                message = "CAC document URL cannot exceed 500 characters"
        )
        String cacDocumentUrl,

        @Size(
                max = 150,
                message = "Bank name cannot exceed 150 characters"
        )
        @Pattern(
                regexp = "^$|^[A-Za-z][A-Za-z .&'\\-]{1,149}$",
                message = "Bank name can contain letters, spaces, apostrophes, hyphens and & only"
        )
        String bankName,

        @Size(
                max = 150,
                message = "Account name cannot exceed 150 characters"
        )
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