package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record AdminSettlementBatchView(
        UUID id,
        Instant periodStart,
        Instant periodEnd,
        String status,
        int shopCount,
        long totalGross,
        long totalCommission,
        long totalTax,
        long totalNet,
        long totalReleased,
        long totalHeld,
        Instant createdAt,
        Instant closedAt,
        String lastError
) {

    public AdminSettlementBatchView {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("period must not be null");
        }

        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException("periodStart must be before periodEnd");
        }

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }

        if (shopCount < 0) {
            throw new IllegalArgumentException("shopCount must not be negative");
        }

        if (totalGross < 0
                || totalCommission < 0
                || totalTax < 0
                || totalNet < 0
                || totalReleased < 0
                || totalHeld < 0) {
            throw new IllegalArgumentException("settlement totals must not be negative");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }

        status = status.trim().toUpperCase();
    }
}