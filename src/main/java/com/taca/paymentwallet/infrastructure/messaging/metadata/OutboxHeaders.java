package com.taca.paymentwallet.infrastructure.messaging.metadata;

import java.util.UUID;

public record OutboxHeaders(
        UUID eventId,
        String requestId,
        String traceparent,
        String tracestate
) {
}