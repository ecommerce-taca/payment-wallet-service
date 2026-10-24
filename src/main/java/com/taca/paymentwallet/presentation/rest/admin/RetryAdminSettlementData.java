package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record RetryAdminSettlementData(
        @JsonProperty("batch_id") UUID batchId,
        String status,
        @JsonProperty("retried_item_count") int retriedItemCount
) {
}