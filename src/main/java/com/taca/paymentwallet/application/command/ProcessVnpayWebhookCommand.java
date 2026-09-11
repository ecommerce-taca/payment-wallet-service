package com.taca.paymentwallet.application.command;

import java.util.Map;
import java.util.UUID;

public record ProcessVnpayWebhookCommand(
        UUID paymentId,
        String providerEventId,
        String providerTransactionRef,
        String responseCode,
        String transactionStatus,
        long amount,
        String currency,
        String payloadHash,
        Map<String, String> signedPayload
) {

    public ProcessVnpayWebhookCommand {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }
        if (providerEventId == null || providerEventId.isBlank()) {
            throw new IllegalArgumentException("providerEventId must not be blank");
        }
        if (providerTransactionRef == null || providerTransactionRef.isBlank()) {
            throw new IllegalArgumentException("providerTransactionRef must not be blank");
        }
        if (responseCode == null || responseCode.isBlank()) {
            throw new IllegalArgumentException("responseCode must not be blank");
        }
        if (transactionStatus == null || transactionStatus.isBlank()) {
            throw new IllegalArgumentException("transactionStatus must not be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        if (payloadHash == null || payloadHash.isBlank()) {
            throw new IllegalArgumentException("payloadHash must not be blank");
        }
        if (signedPayload == null || signedPayload.isEmpty()) {
            throw new IllegalArgumentException("signedPayload must not be empty");
        }

        providerEventId = providerEventId.trim();
        providerTransactionRef = providerTransactionRef.trim();
        responseCode = responseCode.trim();
        transactionStatus = transactionStatus.trim();
        currency = currency.trim().toUpperCase();
        payloadHash = payloadHash.trim();
        signedPayload = Map.copyOf(signedPayload);
    }
}
