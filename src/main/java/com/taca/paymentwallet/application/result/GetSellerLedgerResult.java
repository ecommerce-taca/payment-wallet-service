package com.taca.paymentwallet.application.result;

import java.util.List;

public record GetSellerLedgerResult(
        List<GetSellerLedgerEntryResult> entries,
        int page,
        int size,
        long total,
        int totalPages
) {

    public GetSellerLedgerResult {
        if (entries == null) {
            throw new IllegalArgumentException("entries must not be null");
        }

        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }

        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }

        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages must not be negative");
        }

        entries = List.copyOf(entries);
    }
}