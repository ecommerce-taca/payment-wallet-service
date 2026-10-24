package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminSettlementLineData(
        UUID id,
        @JsonProperty("settlement_batch_item_id")
        UUID settlementBatchItemId,
        @JsonProperty("payment_allocation_id")
        UUID paymentAllocationId,
        @JsonProperty("released_amount")
        long releasedAmount,
        @JsonProperty("created_at")
        Instant createdAt
) {
}