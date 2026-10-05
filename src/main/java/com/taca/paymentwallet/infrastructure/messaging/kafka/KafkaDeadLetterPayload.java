package com.taca.paymentwallet.infrastructure.messaging.kafka;

import java.time.Instant;
import java.util.UUID;

public record KafkaDeadLetterPayload(
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
}