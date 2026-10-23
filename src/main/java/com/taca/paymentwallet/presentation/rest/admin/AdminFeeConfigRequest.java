package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminFeeConfigRequest(
        String scope,

        @JsonProperty("category_id")
        UUID categoryId,

        @JsonProperty("rate_bps")
        int rateBps,

        @JsonProperty("effective_from")
        Instant effectiveFrom,

        String note
) {
}