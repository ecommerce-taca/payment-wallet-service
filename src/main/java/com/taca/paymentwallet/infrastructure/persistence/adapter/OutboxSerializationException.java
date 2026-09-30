package com.taca.paymentwallet.infrastructure.persistence.adapter;

public class OutboxSerializationException extends RuntimeException {

    public OutboxSerializationException(
            String eventType,
            Throwable cause
    ) {
        super(
                "failed to serialize outbox event: " + eventType,
                cause
        );
    }
}