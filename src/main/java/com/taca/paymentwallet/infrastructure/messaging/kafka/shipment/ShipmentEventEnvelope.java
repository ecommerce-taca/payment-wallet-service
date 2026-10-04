package com.taca.paymentwallet.infrastructure.messaging.kafka.shipment;

import java.time.Instant;
import java.util.UUID;

public record ShipmentEventEnvelope(
        String eventId,
        int schemaVersion,
        String eventType,
        Instant occurredAt,
        UUID orderId,
        Instant deliveredAt
) {
}