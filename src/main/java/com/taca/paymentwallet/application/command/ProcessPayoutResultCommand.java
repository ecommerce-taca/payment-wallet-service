package com.taca.paymentwallet.application.command;

import com.taca.paymentwallet.application.payout.PayoutResultStatus;

import java.util.UUID;

public record ProcessPayoutResultCommand(
        UUID payoutId,
        PayoutResultStatus status,
        long amount,
        String currency,
        String providerReference,
        String failureCode
) {

    public ProcessPayoutResultCommand {
        if (payoutId == null) {
            throw new IllegalArgumentException("payoutId must not be null");
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

        if (status == PayoutResultStatus.SUCCESS
                && (providerReference == null || providerReference.isBlank())) {
            throw new IllegalArgumentException("providerReference must not be blank when payout succeeded");
        }

        if (status == PayoutResultStatus.FAILED
                && (failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException("failureCode must not be blank when payout failed");
        }

        currency = currency.trim().toUpperCase();
        providerReference = providerReference == null ? null : providerReference.trim();
        failureCode = failureCode == null ? null : failureCode.trim();
    }
}
