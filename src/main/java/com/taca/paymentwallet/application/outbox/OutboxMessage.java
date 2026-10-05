package com.taca.paymentwallet.application.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OutboxMessage(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        String headers,
        Instant occurredAt,
        int retryCount
) {

    public OutboxMessage {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");

        if (aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType must not be blank");
        }

        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }

        if (payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }

        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
    }
}