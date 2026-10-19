package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminFeeConfigData(
        UUID id,
        String scope,
        @JsonProperty("category_id") UUID categoryId,
        @JsonProperty("rate_bps") int rateBps,
        @JsonProperty("effective_from") Instant effectiveFrom,
        String note,
        @JsonProperty("created_by") UUID createdBy,
        @JsonProperty("created_at") Instant createdAt
) {
}