package com.taca.paymentwallet.application.command;

import com.taca.paymentwallet.application.payment.CodPaymentResultStatus;

import java.time.Instant;
import java.util.UUID;

public record ProcessCodPaymentCommand(
        UUID checkoutGroupId,
        CodPaymentResultStatus status,
        long amount,
        String currency,
        Instant occurredAt,
        String failureCode
) {

    public ProcessCodPaymentCommand {
        if (checkoutGroupId == null) {
            throw new IllegalArgumentException("checkoutGroupId must not be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }

        if ((status == CodPaymentResultStatus.FAILED
                || status == CodPaymentResultStatus.CANCELLED)
                && (failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException("failureCode must not be blank when COD payment failed");
        }

        currency = currency.trim().toUpperCase();
        failureCode = failureCode == null ? null : failureCode.trim().toUpperCase();
    }
}