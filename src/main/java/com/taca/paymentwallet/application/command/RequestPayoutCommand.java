package com.taca.paymentwallet.application.command;

import java.util.UUID;

public record RequestPayoutCommand(
        UUID shopId,
        long amount,
        String currency,
        String bankCode,
        String accountHolderName,
        String maskedAccountNumber,
        String idempotencyKey
) {

    public RequestPayoutCommand {
        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        if (bankCode == null || bankCode.isBlank()) {
            throw new IllegalArgumentException("bankCode must not be blank");
        }

        if (accountHolderName == null || accountHolderName.isBlank()) {
            throw new IllegalArgumentException("accountHolderName must not be blank");
        }

        if (maskedAccountNumber == null || maskedAccountNumber.isBlank()) {
            throw new IllegalArgumentException("maskedAccountNumber must not be blank");
        }
        
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        currency = currency.trim().toUpperCase();
        bankCode = bankCode.trim().toUpperCase();
        accountHolderName = accountHolderName.trim();
        maskedAccountNumber = maskedAccountNumber.trim();
        idempotencyKey = idempotencyKey.trim();
    }
}
