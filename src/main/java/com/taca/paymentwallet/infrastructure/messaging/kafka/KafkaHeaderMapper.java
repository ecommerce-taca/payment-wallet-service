package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.infrastructure.messaging.metadata.OutboxHeaders;
import org.apache.kafka.clients.producer.ProducerRecord;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class KafkaHeaderMapper {

    private final ObjectMapper objectMapper;

    public KafkaHeaderMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public void apply(
            ProducerRecord<String, String> record,
            OutboxMessage message
    ) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(message, "message must not be null");

        add(
                record,
                KafkaHeaderNames.EVENT_ID,
                message.eventId().toString()
        );

        if (message.headers() == null || message.headers().isBlank()) {
            return;
        }

        OutboxHeaders headers;

        try {
            headers = objectMapper.readValue(
                    message.headers(),
                    OutboxHeaders.class
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid outbox headers for event: "
                            + message.eventId(),
                    exception
            );
        }

        add(record, KafkaHeaderNames.REQUEST_ID, headers.requestId());
        add(record, KafkaHeaderNames.TRACEPARENT, headers.traceparent());
        add(record, KafkaHeaderNames.TRACESTATE, headers.tracestate());
    }

    private void add(
            ProducerRecord<String, String> record,
            String name,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        record.headers().add(
                name,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }
}