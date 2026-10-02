package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class KafkaInboundMetadataExtractor {

    public KafkaInboundMetadata extract(
            ConsumerRecord<String, String> record
    ) {
        Objects.requireNonNull(record, "record must not be null");

        String eventId = header(
                record,
                KafkaHeaderNames.EVENT_ID
        );

        if (eventId == null || eventId.isBlank()) {
            throw new KafkaInboundMetadataException(
                    "Kafka record is missing required event_id header"
            );
        }

        return new KafkaInboundMetadata(
                eventId,
                header(record, KafkaHeaderNames.REQUEST_ID),
                header(record, KafkaHeaderNames.TRACEPARENT),
                header(record, KafkaHeaderNames.TRACESTATE)
        );
    }

    private String header(
            ConsumerRecord<String, String> record,
            String name
    ) {
        Header header =
                record.headers().lastHeader(name);

        if (header == null || header.value() == null) {
            return null;
        }

        String value = new String(
                header.value(),
                StandardCharsets.UTF_8
        ).trim();

        return value.isEmpty()
                ? null
                : value;
    }
}