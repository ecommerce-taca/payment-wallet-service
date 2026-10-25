package com.taca.paymentwallet.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Objects;

public class KafkaConsumerObservation {

    static final String CONSUME_TOTAL =
            "payment_wallet_kafka_consume_total";

    static final String CONSUME_LATENCY =
            "payment_wallet_kafka_consume_latency";

    private final MeterRegistry meterRegistry;

    public KafkaConsumerObservation(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    public void record(
            String consumer,
            String eventType,
            String topic,
            String result,
            Duration duration
    ) {
        meterRegistry.counter(
                CONSUME_TOTAL,
                "consumer", consumer,
                "event_type", eventType,
                "topic", topic,
                "result", result
        ).increment();

        Timer.builder(CONSUME_LATENCY)
                .tag("consumer", consumer)
                .tag("event_type", eventType)
                .tag("topic", topic)
                .tag("result", result)
                .register(meterRegistry)
                .record(duration);
    }
}