package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.observability.W3cTraceContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class KafkaInboundMetadataExtractor {

    private final W3cTraceContext traceContext;

    public KafkaInboundMetadata extract(ConsumerRecord<String, String> record) {
        return extract(record, null);
    }

    public KafkaInboundMetadata extract(
            ConsumerRecord<String, String> record,
            String fallbackEventId
    ) {
        Objects.requireNonNull(record, "record must not be null");

        String eventId = header(record, KafkaHeaderNames.EVENT_ID);

        if (eventId == null || eventId.isBlank()) {
            eventId = normalize(fallbackEventId);
        }

        if (eventId == null) {
            throw new KafkaInboundMetadataException(
                    "Kafka record is missing required event_id"
            );
        }

        String traceparent = inboundTraceparent(record);

        return new KafkaInboundMetadata(
                eventId,
                header(record, KafkaHeaderNames.REQUEST_ID),
                traceparent,
                header(record, KafkaHeaderNames.TRACESTATE)
        );
    }

    public KafkaInboundMetadataExtractor(
            W3cTraceContext traceContext
    ) {
        this.traceContext = Objects.requireNonNull(traceContext);
    }

    private String header(
            ConsumerRecord<String, String> record,
            String name
    ) {
        Header header = record.headers().lastHeader(name);

        if (header == null || header.value() == null) {
            return null;
        }

        return normalize(new String(
                header.value(),
                StandardCharsets.UTF_8
        ));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private String inboundTraceparent(
            ConsumerRecord<String, String> record
    ) {
        String value = header(
                record,
                KafkaHeaderNames.TRACEPARENT
        );

        return value == null
                ? traceContext.create()
                : traceContext.childOf(value);
    }
}