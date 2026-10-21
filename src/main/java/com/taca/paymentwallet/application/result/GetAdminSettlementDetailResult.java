package com.taca.paymentwallet.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetAdminSettlementDetailResult(
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
        String lastError,
        List<GetAdminSettlementBatchItemResult> items
) {

    public GetAdminSettlementDetailResult {
        items = List.copyOf(items);
    }
}