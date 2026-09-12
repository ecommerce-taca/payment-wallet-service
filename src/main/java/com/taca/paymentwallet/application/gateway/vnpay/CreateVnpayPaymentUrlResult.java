package com.taca.paymentwallet.application.gateway.vnpay;

import java.time.Instant;

public record CreateVnpayPaymentUrlResult(
        String providerTransactionRef,
        String paymentUrl,
        Instant expiresAt
) {

    public CreateVnpayPaymentUrlResult {
        if (providerTransactionRef == null || providerTransactionRef.isBlank()) {
            throw new IllegalArgumentException("providerTransactionRef must not be blank");
        }

        if (paymentUrl == null || paymentUrl.isBlank()) {
            throw new IllegalArgumentException("paymentUrl must not be blank");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }

        providerTransactionRef = providerTransactionRef.trim();
        paymentUrl = paymentUrl.trim();
    }
}
