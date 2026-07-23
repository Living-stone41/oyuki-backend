package com.oyuki.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankAccountRequest(

        @NotBlank(message = "Bank name is required")
        @Size(
                max = 150,
                message = "Bank name cannot exceed 150 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z .&'\\-]{1,149}$",
                message = "Bank name can contain letters, spaces, apostrophes, hyphens and & only"
        )
        String bankName,

        @NotBlank(message = "Account name is required")
        @Size(
                max = 200,
                message = "Account name cannot exceed 200 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z .'\\-]{1,199}$",
                message = "Account name can contain letters, spaces, apostrophes and hyphens only"
        )
        String accountName,

        @NotBlank(message = "Account number is required")
        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Account number must contain exactly 10 digits"
        )
        String accountNumber,

        @Size(
                max = 2000,
                message = "Payment instructions cannot exceed 2000 characters"
        )
        String paymentInstructions

) {
}
