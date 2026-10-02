package com.taca.paymentwallet.infrastructure.messaging.kafka;

public record KafkaInboundMetadata(
        String eventId,
        String requestId,
        String traceparent,
        String tracestate
) {

    public KafkaInboundMetadata {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
    }
}