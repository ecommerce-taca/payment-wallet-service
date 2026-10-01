package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxDeadLetterPort {

    List<OutboxDeadLetter> lockNextDeadLetterBatch(
            int batchSize,
            int maxRetries,
            Set<String> eventTypes
    );

    void markDeadLettered(UUID eventId, Instant deadLetteredAt);
}