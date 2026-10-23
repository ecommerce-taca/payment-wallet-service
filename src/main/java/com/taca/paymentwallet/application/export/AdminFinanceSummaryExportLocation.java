package com.taca.paymentwallet.application.export;

import java.time.Instant;

public record AdminFinanceSummaryExportLocation(
        String downloadUrl,
        Instant expiresAt
) {

    public AdminFinanceSummaryExportLocation {
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IllegalArgumentException("downloadUrl must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
    }
}