package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxCleanupPropertiesTest {

    @Test
    void shouldCreateValidProperties() {
        OutboxCleanupProperties properties =
                new OutboxCleanupProperties(
                        true,
                        Duration.ofDays(7),
                        500,
                        3_600_000
                );

        assertThat(properties.enabled())
                .isTrue();

        assertThat(properties.retention())
                .isEqualTo(Duration.ofDays(7));

        assertThat(properties.batchSize())
                .isEqualTo(500);

        assertThat(properties.intervalMs())
                .isEqualTo(3_600_000);
    }

    @Test
    void shouldRejectZeroRetention() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxCleanupProperties(
                        true,
                        Duration.ZERO,
                        500,
                        3_600_000
                )
        );
    }

    @Test
    void shouldRejectInvalidBatchSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxCleanupProperties(
                        true,
                        Duration.ofDays(7),
                        0,
                        3_600_000
                )
        );
    }

    @Test
    void shouldRejectInvalidInterval() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxCleanupProperties(
                        true,
                        Duration.ofDays(7),
                        500,
                        0
                )
        );
    }
}