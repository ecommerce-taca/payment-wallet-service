package com.taca.paymentwallet.infrastructure.messaging.kafka.shipment;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ShipmentEventParser {

    public static final String DELIVERED = "shipment.delivered";
    public static final String FAILED = "shipment.failed";

    private final ObjectMapper objectMapper;

    public ShipmentEventParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public ShipmentEventEnvelope parse(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidShipmentEventException(
                    "Shipment event payload must not be blank"
            );
        }

        try {
            JsonNode root = objectMapper.readTree(value);

            String eventId = requiredText(root, "event_id");
            int schemaVersion = requiredInt(root, "schema_version");
            String eventType = requiredText(root, "event_type");
            Instant occurredAt = Instant.parse(
                    requiredText(root, "occurred_at")
            );

            if (!DELIVERED.equals(eventType)
                    && !FAILED.equals(eventType)) {
                throw new InvalidShipmentEventException(
                        "Unsupported shipment event type: " + eventType
                );
            }

            JsonNode payload = root.get("payload");

            if (payload == null || !payload.isObject()) {
                throw new InvalidShipmentEventException(
                        "Shipment event payload object is required"
                );
            }

            UUID orderId = UUID.fromString(
                    requiredText(payload, "order_id")
            );

            Instant deliveredAt = null;

            if (DELIVERED.equals(eventType)) {
                deliveredAt = Instant.parse(
                        requiredText(payload, "delivered_at")
                );
            }

            return new ShipmentEventEnvelope(
                    eventId,
                    schemaVersion,
                    eventType,
                    occurredAt,
                    orderId,
                    deliveredAt
            );
        } catch (InvalidShipmentEventException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidShipmentEventException(
                    "Invalid shipment event payload",
                    exception
            );
        }
    }

    private String requiredText(
            JsonNode node,
            String fieldName
    ) {
        JsonNode value = node.get(fieldName);

        if (value == null
                || !value.isTextual()
                || value.asText().isBlank()) {
            throw new InvalidShipmentEventException(
                    fieldName + " must not be blank"
            );
        }

        return value.asText().trim();
    }

    private int requiredInt(
            JsonNode node,
            String fieldName
    ) {
        JsonNode value = node.get(fieldName);

        if (value == null || !value.isIntegralNumber()) {
            throw new InvalidShipmentEventException(
                    fieldName + " must be an integer"
            );
        }

        return value.asInt();
    }
}