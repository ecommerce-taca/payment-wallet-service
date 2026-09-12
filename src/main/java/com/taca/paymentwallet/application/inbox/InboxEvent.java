package com.taca.paymentwallet.application.inbox;

import java.time.Instant;

public record InboxEvent(
        InboxEventKey key,
        String eventType,
        String payloadHash,
        Instant receivedAt,
        InboxEventStatus status
) {

    public InboxEvent {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }

        if (payloadHash == null || payloadHash.isBlank()) {
            throw new IllegalArgumentException("payloadHash must not be blank");
        }

        if (receivedAt == null) {
            throw new IllegalArgumentException("receivedAt must not be null");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        eventType = eventType.trim();
        payloadHash = payloadHash.trim();
    }
}
