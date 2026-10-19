package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record AdminTaxConfigView(
        UUID id,
        String scope,
        UUID categoryId,
        int rateBps,
        Instant effectiveFrom,
        String note,
        UUID createdBy,
        Instant createdAt
) {

    public AdminTaxConfigView {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }

        if (rateBps < 0 || rateBps > 10_000) {
            throw new IllegalArgumentException("rateBps must be between 0 and 10000");
        }

        if (effectiveFrom == null) {
            throw new IllegalArgumentException("effectiveFrom must not be null");
        }

        if (createdBy == null) {
            throw new IllegalArgumentException("createdBy must not be null");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }

        scope = scope.trim().toUpperCase();
    }
}