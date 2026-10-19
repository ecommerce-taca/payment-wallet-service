package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record AdminSettlementBatchItemView(
        UUID id,
        UUID batchId,
        UUID shopId,
        UUID walletId,
        long gross,
        long commission,
        long tax,
        long net,
        long releasedAmount,
        long heldAmount,
        String status,
        UUID postingId,
        Instant createdAt
) {

    public AdminSettlementBatchItemView {
        if (id == null || batchId == null || shopId == null || walletId == null) {
            throw new IllegalArgumentException("settlement item identifiers must not be null");
        }

        if (gross < 0
                || commission < 0
                || tax < 0
                || net < 0
                || releasedAmount < 0
                || heldAmount < 0) {
            throw new IllegalArgumentException("settlement item amounts must not be negative");
        }

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }

        status = status.trim().toUpperCase();
    }
}