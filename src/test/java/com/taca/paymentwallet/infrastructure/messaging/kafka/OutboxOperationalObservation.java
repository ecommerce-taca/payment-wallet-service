package com.taca.paymentwallet.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxOperationalObservationTest {

    private SimpleMeterRegistry registry;
    private OutboxOperationalObservation observation;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();

        observation =
                new OutboxOperationalObservation(
                        registry
                );
    }

    @Test
    void shouldRecordBacklogGauges() {
        observation.recordBacklog(
                7L,
                Duration.ofSeconds(125)
        );

        assertThat(
                registry.get(
                                OutboxOperationalObservation.PENDING_COUNT
                        )
                        .gauge()
                        .value()
        ).isEqualTo(7.0);

        assertThat(
                registry.get(
                                OutboxOperationalObservation.OLDEST_LAG_SECONDS
                        )
                        .gauge()
                        .value()
        ).isEqualTo(125.0);
    }

    @Test
    void shouldResetBacklogGauges() {
        observation.recordBacklog(
                5L,
                Duration.ofSeconds(60)
        );

        observation.recordBacklog(
                0L,
                Duration.ZERO
        );

        assertThat(
                registry.get(
                                OutboxOperationalObservation.PENDING_COUNT
                        )
                        .gauge()
                        .value()
        ).isZero();

        assertThat(
                registry.get(
                                OutboxOperationalObservation.OLDEST_LAG_SECONDS
                        )
                        .gauge()
                        .value()
        ).isZero();
    }

    @Test
    void shouldRecordCleanupDeletedCount() {
        observation.recordCleanup(12);
        observation.recordCleanup(3);

        assertThat(
                registry.get(
                                OutboxOperationalObservation.CLEANUP_DELETED_TOTAL
                        )
                        .counter()
                        .count()
        ).isEqualTo(15.0);
    }

    @Test
    void shouldRejectNegativePendingCount() {
        assertThatThrownBy(() ->
                observation.recordBacklog(
                        -1L,
                        Duration.ZERO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pending must not be negative");
    }

    @Test
    void shouldRejectNegativeLag() {
        assertThatThrownBy(() ->
                observation.recordBacklog(
                        1L,
                        Duration.ofSeconds(-1)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oldestLag must not be negative");
    }

    @Test
    void shouldRejectNegativeCleanupCount() {
        assertThatThrownBy(() ->
                observation.recordCleanup(-1)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("deleted must not be negative");
    }
}