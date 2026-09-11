package com.taca.paymentwallet.application.result;

import com.taca.paymentwallet.application.settlement.SettlementRunStatus;

import java.util.UUID;

public record RunSettlementResult(
        UUID settlementBatchId,
        SettlementRunStatus status,
        int itemCount,
        long totalReleasedAmount,
        String currency
) {

    public RunSettlementResult {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (status == SettlementRunStatus.COMPLETED && settlementBatchId == null) {
            throw new IllegalArgumentException("settlementBatchId must not be null when settlement completed");
        }

        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must not be negative");
        }

        if (totalReleasedAmount < 0) {
            throw new IllegalArgumentException("totalReleasedAmount must not be negative");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        currency = currency.trim().toUpperCase();
    }

    public static RunSettlementResult empty() {
        return new RunSettlementResult(
                null,
                SettlementRunStatus.EMPTY,
                0,
                0,
                "VND"
        );
    }
}
