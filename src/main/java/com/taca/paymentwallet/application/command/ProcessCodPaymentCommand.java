package com.taca.paymentwallet.application.command;

import com.taca.paymentwallet.application.payment.CodPaymentResultStatus;

import java.time.Instant;
import java.util.UUID;

public record ProcessCodPaymentCommand(
        UUID orderId,
        CodPaymentResultStatus status,
        Instant occurredAt,
        String failureCode
) {

    public ProcessCodPaymentCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }

        if ((status == CodPaymentResultStatus.FAILED
                || status == CodPaymentResultStatus.CANCELLED)
                && (failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException(
                    "failureCode must not be blank when COD payment failed"
            );
        }

        failureCode = failureCode == null
                ? null
                : failureCode.trim().toUpperCase();
    }
}