package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import com.taca.paymentwallet.infrastructure.messaging.metadata.OutboxHeaders;
import org.apache.kafka.clients.producer.ProducerRecord;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class KafkaDeadLetterHeaderMapper {

    private final ObjectMapper objectMapper;

    public KafkaDeadLetterHeaderMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public void apply(
            ProducerRecord<String, String> record,
            OutboxDeadLetter deadLetter
    ) {
        add(
                record,
                KafkaHeaderNames.EVENT_ID,
                deadLetter.eventId().toString()
        );

        if (deadLetter.headers() == null
                || deadLetter.headers().isBlank()) {
            return;
        }

        try {
            OutboxHeaders headers = objectMapper.readValue(
                    deadLetter.headers(),
                    OutboxHeaders.class
            );

            add(
                    record,
                    KafkaHeaderNames.REQUEST_ID,
                    headers.requestId()
            );

            add(
                    record,
                    KafkaHeaderNames.TRACEPARENT,
                    headers.traceparent()
            );

            add(
                    record,
                    KafkaHeaderNames.TRACESTATE,
                    headers.tracestate()
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid outbox headers for DLQ event: "
                            + deadLetter.eventId(),
                    exception
            );
        }
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