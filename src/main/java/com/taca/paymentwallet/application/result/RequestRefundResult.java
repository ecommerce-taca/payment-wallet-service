package com.taca.paymentwallet.application.result;

import java.util.UUID;

public record RequestRefundResult(
        UUID refundId,
        UUID paymentId,
        long amount,
        String currency,
        String status
) {

    public RequestRefundResult {
        if (refundId == null) {
            throw new IllegalArgumentException("refundId must not be null");
        }

        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
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
