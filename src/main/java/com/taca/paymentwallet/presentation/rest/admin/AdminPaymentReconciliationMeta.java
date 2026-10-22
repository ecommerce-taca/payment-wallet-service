package com.taca.paymentwallet.presentation.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminPaymentReconciliationMeta(
        @JsonProperty("request_id")
        String requestId,

        int page,
        int size,
        long total,

        @JsonProperty("total_pages")
        int totalPages,

        @JsonProperty("mismatch_count")
        long mismatchCount
) {

    public AdminPaymentReconciliationMeta {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }

        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }

        if (total < 0 || totalPages < 0 || mismatchCount < 0) {
            throw new IllegalArgumentException("pagination totals must not be negative");
        }

        requestId = requestId.trim();
    }
}