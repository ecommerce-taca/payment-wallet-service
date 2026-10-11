package com.taca.paymentwallet.presentation.rest.seller;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SellerLedgerMeta(
        @JsonProperty("request_id")
        String requestId,

        int page,
        int size,
        long total,

        @JsonProperty("total_pages")
        int totalPages
) {

    public SellerLedgerMeta {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }

        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }

        if (total < 0 || totalPages < 0) {
            throw new IllegalArgumentException("pagination totals must not be negative");
        }

        requestId = requestId.trim();
    }
}