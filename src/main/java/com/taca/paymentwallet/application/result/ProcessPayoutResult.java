package com.taca.paymentwallet.application.result;

import com.taca.paymentwallet.application.payout.PayoutResultProcessingAction;

import java.util.UUID;

public record ProcessPayoutResult(
        UUID payoutId,
        UUID walletId,
        String payoutStatus,
        PayoutResultProcessingAction action
) {

    public ProcessPayoutResult {
        if (payoutId == null) {
            throw new IllegalArgumentException("payoutId must not be null");
        }

        if (walletId == null) {
            throw new IllegalArgumentException("walletId must not be null");
        }

        if (payoutStatus == null || payoutStatus.isBlank()) {
            throw new IllegalArgumentException("payoutStatus must not be blank");
        }

        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }

        payoutStatus = payoutStatus.trim().toUpperCase();
    }
}