package com.taca.paymentwallet.application.command;

import com.taca.paymentwallet.application.refund.RefundResultStatus;

import java.util.UUID;

public record ProcessRefundResultCommand(
        UUID refundId,
        RefundResultStatus status,
        long amount,
        String currency,
        String providerReference,
        String failureCode
) {

    public ProcessRefundResultCommand {
        if (refundId == null) {
            throw new IllegalArgumentException("refundId must not be null");
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

        if (status == RefundResultStatus.SUCCESS
                && (providerReference == null || providerReference.isBlank())) {
            throw new IllegalArgumentException("providerReference must not be blank when refund succeeded");
        }
        
        if (status == RefundResultStatus.FAILED
                && (failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException("failureCode must not be blank when refund failed");
        }

        currency = currency.trim().toUpperCase();
        providerReference = providerReference == null ? null : providerReference.trim();
        failureCode = failureCode == null ? null : failureCode.trim();
    }
}
