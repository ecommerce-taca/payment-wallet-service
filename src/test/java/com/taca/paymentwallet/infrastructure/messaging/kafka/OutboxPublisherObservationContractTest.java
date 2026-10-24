package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPublisherObservationContractTest {

    @Test
    void shouldKeepAuthoritativeMetricNames() {
        assertThat(
                OutboxPublisherObservation.PUBLISH_TOTAL
        ).isEqualTo(
                "payment_wallet_outbox_publish_total"
        );

        assertThat(
                OutboxPublisherObservation.PUBLISH_LATENCY
        ).isEqualTo(
                "payment_wallet_outbox_publish_latency"
        );

        assertThat(
                OutboxPublisherObservation.DLQ_TOTAL
        ).isEqualTo(
                "payment_wallet_outbox_dlq_publish_total"
        );
    }

    @Test
    void shouldRecordPublishWithBoundedTags() {
        io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

        OutboxPublisherObservation observation =
                new OutboxPublisherObservation(registry);

        observation.recordPublish(
                "payment.succeeded",
                "payment.events.v1",
                "success",
                java.time.Duration.ofMillis(25)
        );

        assertThat(
                registry.get(
                                OutboxPublisherObservation.PUBLISH_TOTAL
                        )
                        .tags(
                                "event_type",
                                "payment.succeeded",
                                "topic",
                                "payment.events.v1",
                                "result",
                                "success"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get(
                                OutboxPublisherObservation.PUBLISH_LATENCY
                        )
                        .tags(
                                "event_type",
                                "payment.succeeded",
                                "topic",
                                "payment.events.v1",
                                "result",
                                "success"
                        )
                        .timer()
                        .count()
        ).isEqualTo(1L);
    }

    @Test
    void shouldRecordDeadLetterWithBoundedTags() {
        io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

        OutboxPublisherObservation observation =
                new OutboxPublisherObservation(registry);

        observation.recordDeadLetter(
                "payment.failed",
                "payment.events.v1",
                "success",
                java.time.Duration.ofMillis(15)
        );

        assertThat(
                registry.get(
                                OutboxPublisherObservation.DLQ_TOTAL
                        )
                        .tags(
                                "event_type",
                                "payment.failed",
                                "topic",
                                "payment.events.v1",
                                "result",
                                "success"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }
}