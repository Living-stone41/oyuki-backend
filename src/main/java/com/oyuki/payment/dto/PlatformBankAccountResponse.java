package com.oyuki.payment.dto;

import com.oyuki.payment.entity.PlatformBankAccount;

import java.time.LocalDateTime;

public record PlatformBankAccountResponse(

        Long id,
        String bankName,
        String accountName,
        String accountNumber,
        String paymentInstructions,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static PlatformBankAccountResponse from(
            PlatformBankAccount account
    ) {
        return new PlatformBankAccountResponse(
                account.getId(),
                account.getBankName(),
                account.getAccountName(),
                account.getAccountNumber(),
                account.getPaymentInstructions(),
                account.isActive(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}