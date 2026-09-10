package com.taca.paymentwallet.application.idempotency;

import java.time.Instant;

public record IdempotencyRecord(
        IdempotencyScope scope,
        String idempotencyKey,
        String requestHash,
        IdempotencyStatus status,
        String responsePayload,
        String failureCode,
        Instant createdAt,
        Instant updatedAt
) {

    public IdempotencyRecord {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        if (requestHash == null || requestHash.isBlank()) {
            throw new IllegalArgumentException("requestHash must not be blank");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }

        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }

        idempotencyKey = idempotencyKey.trim();
        requestHash = requestHash.trim();
    }

    public boolean isProcessing() {
        return status == IdempotencyStatus.PROCESSING;
    }

    public boolean isSucceeded() {
        return status == IdempotencyStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == IdempotencyStatus.FAILED;
    }
}
