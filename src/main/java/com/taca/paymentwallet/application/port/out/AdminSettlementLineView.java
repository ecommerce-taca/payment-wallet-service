package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record AdminSettlementLineView(
        UUID id,
        UUID settlementBatchItemId,
        UUID paymentAllocationId,
        long releasedAmount,
        Instant createdAt
) {

    public AdminSettlementLineView {
        if (id == null
                || settlementBatchItemId == null
                || paymentAllocationId == null) {
            throw new IllegalArgumentException("settlement line identifiers must not be null");
        }

        if (releasedAmount <= 0) {
            throw new IllegalArgumentException("releasedAmount must be positive");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
    }
}