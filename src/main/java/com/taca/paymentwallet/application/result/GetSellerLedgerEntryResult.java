package com.taca.paymentwallet.application.result;

import java.time.Instant;
import java.util.UUID;

public record GetSellerLedgerEntryResult(
        UUID entryId,
        UUID postingId,
        String entryType,
        long amount,
        long balanceAfter,
        String referenceType,
        UUID referenceId,
        String description,
        Instant createdAt
) {

    public GetSellerLedgerEntryResult {
        if (entryId == null) {
            throw new IllegalArgumentException("entryId must not be null");
        }

        if (postingId == null) {
            throw new IllegalArgumentException("postingId must not be null");
        }

        if (!"DEBIT".equals(entryType) && !"CREDIT".equals(entryType)) {
            throw new IllegalArgumentException("entryType must be DEBIT or CREDIT");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (balanceAfter < 0) {
            throw new IllegalArgumentException("balanceAfter must not be negative");
        }

        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType must not be blank");
        }

        if (referenceId == null) {
            throw new IllegalArgumentException("referenceId must not be null");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }

        entryType = entryType.trim().toUpperCase();
        referenceType = referenceType.trim().toUpperCase();
        description = description == null ? null : description.trim();
    }
}