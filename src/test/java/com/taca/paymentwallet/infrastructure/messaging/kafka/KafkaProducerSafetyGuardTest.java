package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class KafkaProducerSafetyGuardTest {

    private static final long SEND_TIMEOUT_MS =
            35_000;

    @Test
    void shouldAcceptSafeProducerConfiguration() {
        assertDoesNotThrow(
                () -> new KafkaProducerSafetyGuard(
                        safeProperties(),
                        SEND_TIMEOUT_MS
                )
        );
    }

    @Test
    void shouldRejectUnsafeAcks() {
        Map<String, Object> properties =
                safeProperties();

        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "1"
        );

        assertThatThrownBy(
                () -> new KafkaProducerSafetyGuard(
                        properties,
                        SEND_TIMEOUT_MS
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Kafka producer acks must be all"
                );
    }

    @Test
    void shouldRejectDisabledIdempotence() {
        Map<String, Object> properties =
                safeProperties();

        properties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                false
        );

        assertThatThrownBy(
                () -> new KafkaProducerSafetyGuard(
                        properties,
                        SEND_TIMEOUT_MS
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Kafka producer enable.idempotence must be true"
                );
    }

    @Test
    void shouldRejectZeroRetries() {
        Map<String, Object> properties =
                safeProperties();

        properties.put(
                ProducerConfig.RETRIES_CONFIG,
                0
        );

        assertThatThrownBy(
                () -> new KafkaProducerSafetyGuard(
                        properties,
                        SEND_TIMEOUT_MS
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Kafka producer retries must be >= 1"
                );
    }

    @Test
    void shouldRejectTooManyInFlightRequests() {
        Map<String, Object> properties =
                safeProperties();

        properties.put(
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                6
        );

        assertThatThrownBy(
                () -> new KafkaProducerSafetyGuard(
                        properties,
                        SEND_TIMEOUT_MS
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Kafka producer max.in.flight.requests.per.connection must be <= 5"
                );
    }

    @Test
    void shouldRejectDeliveryTimeoutNotGreaterThanRequestTimeout() {
        Map<String, Object> properties =
                safeProperties();

        properties.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                10_000
        );

        properties.put(
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                10_000
        );

        assertThatThrownBy(
                () -> new KafkaProducerSafetyGuard(
                        properties,
                        SEND_TIMEOUT_MS
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Kafka producer delivery.timeout.ms must be greater than request.timeout.ms"
                );
    }

    @Test
    void shouldRejectOutboxSendTimeoutNotGreaterThanDeliveryTimeout() {
        Map<String, Object> properties =
                safeProperties();

        assertThatThrownBy(
                () -> new KafkaProducerSafetyGuard(
                        properties,
                        30_000
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Outbox send-timeout-ms must be greater than Kafka delivery.timeout.ms"
                );
    }

    @Test
    void shouldAcceptMinusOneAcks() {
        Map<String, Object> properties =
                safeProperties();

        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "-1"
        );

        assertDoesNotThrow(
                () -> new KafkaProducerSafetyGuard(
                        properties,
                        SEND_TIMEOUT_MS
                )
        );
    }

    private Map<String, Object> safeProperties() {
        Map<String, Object> properties =
                new HashMap<>();

        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        properties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        properties.put(
                ProducerConfig.RETRIES_CONFIG,
                10
        );

        properties.put(
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                5
        );

        properties.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                30_000
        );

        properties.put(
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                10_000
        );

        return properties;
    }
}