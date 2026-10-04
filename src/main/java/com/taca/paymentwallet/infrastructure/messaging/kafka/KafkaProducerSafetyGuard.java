package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Map;
import java.util.Objects;

public class KafkaProducerSafetyGuard {

    public KafkaProducerSafetyGuard(
            Map<String, Object> producerProperties,
            long sendTimeoutMs
    ) {
        Objects.requireNonNull(
                producerProperties,
                "producerProperties must not be null"
        );

        validate(
                producerProperties,
                sendTimeoutMs
        );
    }

    private void validate(
            Map<String, Object> properties,
            long sendTimeoutMs
    ) {
        String acks = required(
                properties,
                ProducerConfig.ACKS_CONFIG
        );

        if (!"all".equalsIgnoreCase(acks)
                && !"-1".equals(acks)) {
            throw new IllegalStateException(
                    "Kafka producer acks must be all"
            );
        }

        boolean idempotence = Boolean.parseBoolean(
                required(
                        properties,
                        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG
                )
        );

        if (!idempotence) {
            throw new IllegalStateException(
                    "Kafka producer enable.idempotence must be true"
            );
        }

        int retries = requiredInt(
                properties,
                ProducerConfig.RETRIES_CONFIG
        );

        if (retries < 1) {
            throw new IllegalStateException(
                    "Kafka producer retries must be >= 1"
            );
        }

        int maxInFlight = requiredInt(
                properties,
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION
        );

        if (maxInFlight > 5) {
            throw new IllegalStateException(
                    "Kafka producer max.in.flight.requests.per.connection must be <= 5"
            );
        }

        int deliveryTimeoutMs = requiredInt(
                properties,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG
        );

        int requestTimeoutMs = requiredInt(
                properties,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG
        );

        if (deliveryTimeoutMs <= requestTimeoutMs) {
            throw new IllegalStateException(
                    "Kafka producer delivery.timeout.ms must be greater than request.timeout.ms"
            );
        }

        if (sendTimeoutMs <= deliveryTimeoutMs) {
            throw new IllegalStateException(
                    "Outbox send-timeout-ms must be greater than Kafka delivery.timeout.ms"
            );
        }
    }

    private String required(
            Map<String, Object> properties,
            String name
    ) {
        Object value = properties.get(name);

        if (value == null
                || value.toString().isBlank()) {
            throw new IllegalStateException(
                    "Missing Kafka producer property: "
                            + name
            );
        }

        return value.toString().trim();
    }

    private int requiredInt(
            Map<String, Object> properties,
            String name
    ) {
        String value = required(
                properties,
                name
        );

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Kafka producer property "
                            + name
                            + " must be an integer",
                    exception
            );
        }
    }
}