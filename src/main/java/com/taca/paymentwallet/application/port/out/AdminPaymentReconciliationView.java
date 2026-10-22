package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record AdminPaymentReconciliationView(
        UUID paymentId,
        String provider,
        String localStatus,
        String providerStatus,
        long amount,
        boolean matched,
        Instant checkedAt
) {

    public AdminPaymentReconciliationView {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }

        if (localStatus == null || localStatus.isBlank()) {
            throw new IllegalArgumentException("localStatus must not be blank");
        }

        if (providerStatus == null || providerStatus.isBlank()) {
            throw new IllegalArgumentException("providerStatus must not be blank");
        }

        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        if (checkedAt == null) {
            throw new IllegalArgumentException("checkedAt must not be null");
        }

        provider = provider.trim().toUpperCase();
        localStatus = localStatus.trim().toUpperCase();
        providerStatus = providerStatus.trim().toUpperCase();
    }
}