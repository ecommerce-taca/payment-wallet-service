package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaInboundMetadataExtractorTest {

    private final KafkaInboundMetadataExtractor extractor =
            new KafkaInboundMetadataExtractor();

    @Test
    void shouldExtractKafkaMetadata() {
        ConsumerRecord<String, String> record =
                record();

        record.headers().add(
                KafkaHeaderNames.EVENT_ID,
                bytes("event-001")
        );

        record.headers().add(
                KafkaHeaderNames.REQUEST_ID,
                bytes("req-001")
        );

        record.headers().add(
                KafkaHeaderNames.TRACEPARENT,
                bytes(
                        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
                )
        );

        record.headers().add(
                KafkaHeaderNames.TRACESTATE,
                bytes("vendor=value")
        );

        KafkaInboundMetadata metadata =
                extractor.extract(record);

        assertThat(metadata.eventId())
                .isEqualTo("event-001");

        assertThat(metadata.requestId())
                .isEqualTo("req-001");

        assertThat(metadata.traceparent())
                .startsWith("00-");

        assertThat(metadata.tracestate())
                .isEqualTo("vendor=value");
    }

    @Test
    void shouldRejectRecordWithoutEventId() {
        ConsumerRecord<String, String> record =
                record();

        assertThrows(
                KafkaInboundMetadataException.class,
                () -> extractor.extract(record)
        );
    }

    @Test
    void shouldUseFallbackEventIdWhenHeaderIsMissing() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(
                        "shipment.events.v1",
                        0,
                        0L,
                        "order-key",
                        "{}"
                );

        KafkaInboundMetadata metadata =
                extractor.extract(
                        record,
                        "shipment-event-001"
                );

        assertThat(metadata.eventId())
                .isEqualTo("shipment-event-001");
    }

    @Test
    void shouldPreferHeaderEventIdOverFallbackEventId() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(
                        "shipment.events.v1",
                        0,
                        0L,
                        "order-key",
                        "{}"
                );

        record.headers().add(
                KafkaHeaderNames.EVENT_ID,
                "header-event-001".getBytes(StandardCharsets.UTF_8)
        );

        KafkaInboundMetadata metadata =
                extractor.extract(
                        record,
                        "payload-event-001"
                );

        assertThat(metadata.eventId())
                .isEqualTo("header-event-001");
    }

    private ConsumerRecord<String, String> record() {
        return new ConsumerRecord<>(
                "shipment-topic",
                0,
                1L,
                "key",
                "{}"
        );
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}