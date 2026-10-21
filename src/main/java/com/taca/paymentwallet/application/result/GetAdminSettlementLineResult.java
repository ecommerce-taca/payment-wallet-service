package com.taca.paymentwallet.application.result;

import java.time.Instant;
import java.util.UUID;

public record GetAdminSettlementLineResult(
        UUID id,
        UUID settlementBatchItemId,
        UUID paymentAllocationId,
        long releasedAmount,
        Instant createdAt
) {
}