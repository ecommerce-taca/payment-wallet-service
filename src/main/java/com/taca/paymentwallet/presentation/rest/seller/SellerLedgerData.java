package com.taca.paymentwallet.presentation.rest.seller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SellerLedgerData(
        @JsonProperty("entry_id")
        UUID entryId,

        @JsonProperty("posting_id")
        UUID postingId,

        @JsonProperty("entry_type")
        String entryType,

        long amount,

        @JsonProperty("balance_after")
        long balanceAfter,

        SellerLedgerReferenceData reference,

        String description,

        @JsonProperty("created_at")
        Instant createdAt
) {
}