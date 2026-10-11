package com.taca.paymentwallet.application.query;

import java.time.Instant;
import java.util.UUID;

public record GetSellerLedgerQuery(
        UUID shopId,
        int page,
        int size,
        Instant from,
        Instant to,
        String type
) {

    public GetSellerLedgerQuery {
        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }

        type = type == null || type.isBlank()
                ? null
                : type.trim().toUpperCase();
    }
}