package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.outbox.OutboxMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxPublishingPort {

    List<OutboxMessage> lockNextBatch(int batchSize, int maxRetries);

    void markPublished(UUID eventId, Instant publishedAt);

    void recordFailure(UUID eventId, String error);
}