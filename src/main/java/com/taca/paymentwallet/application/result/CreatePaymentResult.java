package com.taca.paymentwallet.application.result;

import java.time.Instant;
import java.util.UUID;

public record CreatePaymentResult(
        UUID paymentId,
        UUID checkoutGroupId,
        String status,
        String method,
        long amount,
        String currency,
        String paymentUrl,
        Instant expiresAt
) {

    public CreatePaymentResult {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (checkoutGroupId == null) {
            throw new IllegalArgumentException("checkoutGroupId must not be null");
        }

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }

        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (!"VND".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("currency must be VND");
        }

        status = status.trim().toUpperCase();

        method = method.trim().toUpperCase();

        currency = "VND";

        paymentUrl =
                paymentUrl == null
                        ? null
                        : paymentUrl.trim();

        if ("VNPAY".equals(method) && expiresAt == null) {
            throw new IllegalArgumentException(
                    "expiresAt must not be null for VNPAY"
            );
        }

        if ("COD".equals(method) && expiresAt != null) {
            throw new IllegalArgumentException(
                    "expiresAt must be null for COD"
            );
        }
    }
}