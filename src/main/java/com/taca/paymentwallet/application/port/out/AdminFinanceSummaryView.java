package com.taca.paymentwallet.application.port.out;

import java.time.Instant;

public record AdminFinanceSummaryView(
        long capturedAmount,
        long refundedAmount,
        long grossAmount,
        long commissionAmount,
        long taxAmount,
        long sellerNetAmount,
        long availableBalance,
        long pendingBalance,
        long completedPayoutAmount,
        Instant asOf
) {

    public AdminFinanceSummaryView {
        if (capturedAmount < 0
                || refundedAmount < 0
                || grossAmount < 0
                || commissionAmount < 0
                || taxAmount < 0
                || sellerNetAmount < 0
                || availableBalance < 0
                || pendingBalance < 0
                || completedPayoutAmount < 0) {
            throw new IllegalArgumentException(
                    "finance summary amounts must not be negative"
            );
        }

        if (asOf == null) {
            throw new IllegalArgumentException(
                    "asOf must not be null"
            );
        }
    }
}