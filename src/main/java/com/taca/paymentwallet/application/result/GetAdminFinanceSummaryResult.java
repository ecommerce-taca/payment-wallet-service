package com.taca.paymentwallet.application.result;

import java.time.Instant;

public record GetAdminFinanceSummaryResult(
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
}