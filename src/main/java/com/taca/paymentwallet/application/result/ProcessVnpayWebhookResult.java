package com.taca.paymentwallet.application.result;

import java.util.UUID;

public record ProcessVnpayWebhookResult(
        UUID paymentId,
        String paymentStatus,
        WebhookProcessingAction action
) {

    public ProcessVnpayWebhookResult {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }
        if (paymentStatus == null || paymentStatus.isBlank()) {
            throw new IllegalArgumentException("paymentStatus must not be blank");
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }

        paymentStatus = paymentStatus.trim().toUpperCase();
    }
}
