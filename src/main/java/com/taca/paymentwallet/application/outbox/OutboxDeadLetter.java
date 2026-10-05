package com.taca.paymentwallet.application.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OutboxDeadLetter(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        String headers,
        Instant occurredAt,
        int retryCount,
        String lastError
) {

    public OutboxDeadLetter {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");

        if (retryCount <= 0) {
            throw new IllegalArgumentException("retryCount must be positive");
        }

        if (lastError == null || lastError.isBlank()) {
            throw new IllegalArgumentException("lastError must not be blank");
        }
    }
}