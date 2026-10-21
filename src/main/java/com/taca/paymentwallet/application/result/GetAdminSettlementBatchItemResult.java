package com.taca.paymentwallet.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetAdminSettlementBatchItemResult(
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
        Instant createdAt,
        List<GetAdminSettlementLineResult> lines
) {

    public GetAdminSettlementBatchItemResult {
        lines = List.copyOf(lines);
    }
}