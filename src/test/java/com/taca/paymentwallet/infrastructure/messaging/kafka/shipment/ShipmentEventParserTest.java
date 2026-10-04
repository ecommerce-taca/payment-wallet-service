package com.taca.paymentwallet.infrastructure.messaging.kafka.shipment;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentEventParserTest {

    private final ShipmentEventParser parser =
            new ShipmentEventParser(new ObjectMapper());

    @Test
    void shouldParseDeliveredEvent() {
        UUID eventId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        String json = """
                {
                  "event_id":"%s",
                  "schema_version":1,
                  "event_type":"shipment.delivered",
                  "occurred_at":"2026-10-03T10:00:00Z",
                  "aggregate_type":"SHIPMENT",
                  "aggregate_id":"%s",
                  "payload":{
                    "shipment_id":"%s",
                    "order_id":"%s",
                    "delivered_at":"2026-10-03T09:59:00Z"
                  }
                }
                """.formatted(
                eventId,
                shipmentId,
                shipmentId,
                orderId
        );

        ShipmentEventEnvelope event = parser.parse(json);

        assertThat(event.eventId())
                .isEqualTo(eventId.toString());

        assertThat(event.schemaVersion())
                .isEqualTo(1);

        assertThat(event.eventType())
                .isEqualTo("shipment.delivered");

        assertThat(event.orderId())
                .isEqualTo(orderId);

        assertThat(event.occurredAt())
                .isEqualTo(
                        Instant.parse("2026-10-03T10:00:00Z")
                );

        assertThat(event.deliveredAt())
                .isEqualTo(
                        Instant.parse("2026-10-03T09:59:00Z")
                );
    }

    @Test
    void shouldParseFailedEventWithoutTrustingMonetaryFields() {
        UUID eventId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        String json = """
                {
                  "event_id":"%s",
                  "schema_version":1,
                  "event_type":"shipment.failed",
                  "occurred_at":"2026-10-03T10:00:00Z",
                  "aggregate_type":"SHIPMENT",
                  "aggregate_id":"%s",
                  "payload":{
                    "shipment_id":"%s",
                    "order_id":"%s",
                    "reason":"DELIVERY_FAILED",
                    "source":"GHN"
                  }
                }
                """.formatted(
                eventId,
                shipmentId,
                shipmentId,
                orderId
        );

        ShipmentEventEnvelope event = parser.parse(json);

        assertThat(event.eventType())
                .isEqualTo("shipment.failed");

        assertThat(event.orderId())
                .isEqualTo(orderId);

        assertThat(event.deliveredAt())
                .isNull();

        assertThat(event.occurredAt())
                .isEqualTo(
                        Instant.parse("2026-10-03T10:00:00Z")
                );
    }

    @Test
    void shouldRejectEventWithoutOrderId() {
        String json = """
                {
                  "event_id":"event-001",
                  "schema_version":1,
                  "event_type":"shipment.delivered",
                  "occurred_at":"2026-10-03T10:00:00Z",
                  "payload":{
                    "delivered_at":"2026-10-03T09:59:00Z"
                  }
                }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidShipmentEventException.class)
                .hasMessageContaining("order_id");
    }

    @Test
    void shouldRejectUnsupportedShipmentEvent() {
        UUID orderId = UUID.randomUUID();

        String json = """
                {
                  "event_id":"event-001",
                  "schema_version":1,
                  "event_type":"shipment.created",
                  "occurred_at":"2026-10-03T10:00:00Z",
                  "payload":{
                    "order_id":"%s"
                  }
                }
                """.formatted(orderId);

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidShipmentEventException.class)
                .hasMessageContaining(
                        "Unsupported shipment event type"
                );
    }
}