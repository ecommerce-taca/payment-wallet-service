package com.taca.paymentwallet.application.result;

import java.util.UUID;

public record CreatePaymentResult(
        UUID paymentId,
        String status,
        String paymentUrl
) {

    public CreatePaymentResult {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }

        status = status.trim().toUpperCase();
        paymentUrl = paymentUrl == null ? null : paymentUrl.trim();
    }
}
