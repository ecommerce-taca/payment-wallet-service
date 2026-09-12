package com.taca.paymentwallet.application.paymentevent;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;

public record PaymentProviderEvent(
        String provider,
        String providerEventId,
        String providerTransactionRef,
        PaymentId paymentId,
        String responseCode,
        String transactionStatus,
        Money amount,
        String payloadHash,
        Instant receivedAt,
        PaymentProviderEventStatus status
) {

    public PaymentProviderEvent {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (providerEventId == null || providerEventId.isBlank()) {
            throw new IllegalArgumentException("providerEventId must not be blank");
        }
        if (providerTransactionRef == null || providerTransactionRef.isBlank()) {
            throw new IllegalArgumentException("providerTransactionRef must not be blank");
        }
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }
        if (responseCode == null || responseCode.isBlank()) {
            throw new IllegalArgumentException("responseCode must not be blank");
        }
        if (transactionStatus == null || transactionStatus.isBlank()) {
            throw new IllegalArgumentException("transactionStatus must not be blank");
        }
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (payloadHash == null || payloadHash.isBlank()) {
            throw new IllegalArgumentException("payloadHash must not be blank");
        }
        if (receivedAt == null) {
            throw new IllegalArgumentException("receivedAt must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        provider = provider.trim().toUpperCase();
        providerEventId = providerEventId.trim();
        providerTransactionRef = providerTransactionRef.trim();
        responseCode = responseCode.trim();
        transactionStatus = transactionStatus.trim();
        payloadHash = payloadHash.trim();
    }
}