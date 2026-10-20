package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminSettlementBatchData(
        UUID id,
        @JsonProperty("period_start") Instant periodStart,
        @JsonProperty("period_end") Instant periodEnd,
        String status,
        @JsonProperty("shop_count") int shopCount,
        @JsonProperty("total_gross") long totalGross,
        @JsonProperty("total_commission") long totalCommission,
        @JsonProperty("total_tax") long totalTax,
        @JsonProperty("total_net") long totalNet,
        @JsonProperty("total_released") long totalReleased,
        @JsonProperty("total_held") long totalHeld,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("closed_at") Instant closedAt,
        @JsonProperty("last_error") String lastError
) {
}