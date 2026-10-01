package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxPublisherPropertiesTest {

    @Test
    void shouldCreateValidProperties() {
        OutboxPublisherProperties properties =
                new OutboxPublisherProperties(
                        true,
                        50,
                        3,
                        Duration.ofSeconds(2),
                        1000
                );

        assertThat(properties.batchSize()).isEqualTo(50);
        assertThat(properties.maxRetries()).isEqualTo(3);
        assertThat(properties.retryBackoff())
                .isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.pollIntervalMs()).isEqualTo(1000);
    }

    @Test
    void shouldRejectInvalidBatchSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxPublisherProperties(
                        true,
                        0,
                        3,
                        Duration.ofSeconds(2),
                        1000
                )
        );
    }

    @Test
    void shouldRejectInvalidRetryBackoff() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxPublisherProperties(
                        true,
                        50,
                        3,
                        Duration.ZERO,
                        1000
                )
        );
    }
}