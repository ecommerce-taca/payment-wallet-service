package com.taca.paymentwallet.application.result;

import java.util.UUID;

public record RequestPayoutResult(
        UUID payoutId,
        UUID walletId,
        UUID shopId,
        long amount,
        String currency,
        String status
) {

    public RequestPayoutResult {
        if (payoutId == null) {
            throw new IllegalArgumentException("payoutId must not be null");
        }

        if (walletId == null) {
            throw new IllegalArgumentException("walletId must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }

        currency = currency.trim().toUpperCase();
        status = status.trim().toUpperCase();
    }
}