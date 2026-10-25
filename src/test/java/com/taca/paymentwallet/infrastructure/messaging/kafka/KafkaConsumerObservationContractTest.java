package com.taca.paymentwallet.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerObservationContractTest {

    @Test
    void shouldKeepAuthoritativeMetricNames() {
        assertThat(KafkaConsumerObservation.CONSUME_TOTAL)
                .isEqualTo("payment_wallet_kafka_consume_total");

        assertThat(KafkaConsumerObservation.CONSUME_LATENCY)
                .isEqualTo("payment_wallet_kafka_consume_latency");
    }

    @Test
    void shouldRecordConsumeWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KafkaConsumerObservation observation =
                new KafkaConsumerObservation(registry);

        observation.record(
                "payment-wallet-shipment-consumer",
                "shipment.delivered",
                "shipment.events.v1",
                "success",
                Duration.ofMillis(20)
        );

        assertThat(
                registry.get(KafkaConsumerObservation.CONSUME_TOTAL)
                        .tags(
                                "consumer", "payment-wallet-shipment-consumer",
                                "event_type", "shipment.delivered",
                                "topic", "shipment.events.v1",
                                "result", "success"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get(KafkaConsumerObservation.CONSUME_LATENCY)
                        .tags(
                                "consumer", "payment-wallet-shipment-consumer",
                                "event_type", "shipment.delivered",
                                "topic", "shipment.events.v1",
                                "result", "success"
                        )
                        .timer()
                        .count()
        ).isEqualTo(1L);
    }
}