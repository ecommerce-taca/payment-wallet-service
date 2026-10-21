package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminSettlementBatchItemData(
        UUID id,
        @JsonProperty("batch_id") UUID batchId,
        @JsonProperty("shop_id") UUID shopId,
        @JsonProperty("wallet_id") UUID walletId,
        long gross,
        long commission,
        long tax,
        long net,
        @JsonProperty("released_amount") long releasedAmount,
        @JsonProperty("held_amount") long heldAmount,
        String status,
        @JsonProperty("posting_id") UUID postingId,
        @JsonProperty("created_at") Instant createdAt,
        List<AdminSettlementLineData> lines
) {
}