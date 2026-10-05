package com.taca.paymentwallet.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Objects;

public class OutboxPublisherObservation {

    static final String PUBLISH_TOTAL =
            "payment_wallet_outbox_publish_total";

    static final String PUBLISH_LATENCY =
            "payment_wallet_outbox_publish_latency";

    static final String DLQ_TOTAL =
            "payment_wallet_outbox_dlq_publish_total";

    private final MeterRegistry meterRegistry;

    public OutboxPublisherObservation(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry =
                Objects.requireNonNull(meterRegistry);
    }

    public void recordPublish(
            String eventType,
            String topic,
            String result,
            Duration duration
    ) {
        meterRegistry.counter(
                PUBLISH_TOTAL,
                "event_type", eventType,
                "topic", topic,
                "result", result
        ).increment();

        Timer.builder(PUBLISH_LATENCY)
                .tag("event_type", eventType)
                .tag("topic", topic)
                .tag("result", result)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordDeadLetter(
            String eventType,
            String topic,
            String result,
            Duration duration
    ) {
        meterRegistry.counter(
                DLQ_TOTAL,
                "event_type", eventType,
                "topic", topic,
                "result", result
        ).increment();

        Timer.builder(PUBLISH_LATENCY)
                .tag("event_type", eventType)
                .tag("topic", topic)
                .tag("result", result)
                .register(meterRegistry)
                .record(duration);
    }
}