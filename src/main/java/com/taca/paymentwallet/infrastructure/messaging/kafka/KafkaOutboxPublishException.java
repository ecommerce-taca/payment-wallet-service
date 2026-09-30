package com.taca.paymentwallet.infrastructure.messaging.kafka;

import java.util.UUID;

public class KafkaOutboxPublishException extends RuntimeException {

    public KafkaOutboxPublishException(UUID eventId, Throwable cause) {
        super("Failed to publish outbox event: " + eventId, cause);
    }
}