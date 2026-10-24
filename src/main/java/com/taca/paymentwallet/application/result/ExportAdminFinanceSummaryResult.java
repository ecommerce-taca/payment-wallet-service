package com.taca.paymentwallet.application.result;

import java.time.Instant;

public record ExportAdminFinanceSummaryResult(
        String downloadUrl,
        Instant expiresAt
) {
}