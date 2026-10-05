package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.outbox.OutboxMessage;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxPublishingPort {

    List<OutboxMessage> lockNextBatch(
            int batchSize,
            int maxRetries,
            Instant now,
            Set<String> eventTypes
    );

    void markPublished(UUID eventId, Instant publishedAt);

    void recordFailure(
            UUID eventId,
            String error,
            Instant nextAttemptAt
    );
}