package com.taca.paymentwallet.application.command;
import java.util.UUID;

public record RequestRefundCommand(
        UUID paymentId,
        long amount,
        String currency,
        String reason,
        String idempotencyKey
) {

    public RequestRefundCommand {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        currency = currency.trim().toUpperCase();
        reason = reason.trim();
        idempotencyKey = idempotencyKey.trim();
    }
}
