package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaHeaderMapperTest {

    private final KafkaHeaderMapper mapper =
            new KafkaHeaderMapper(
                    new ObjectMapper()
            );

    @Test
    void shouldAddEventAndTracingHeaders() {
        UUID eventId = UUID.randomUUID();

        OutboxMessage message = new OutboxMessage(
                eventId,
                "PAYMENT",
                UUID.randomUUID(),
                "payment.created",
                "{}",
                """
                {
                  "eventId":"%s",
                  "requestId":"req-001",
                  "traceparent":"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                  "tracestate":"vendor=value"
                }
                """.formatted(eventId),
                Instant.parse("2026-10-01T10:00:00Z"),
                0
        );

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        "payment.events.v1",
                        "key",
                        "{}"
                );

        mapper.apply(record, message);

        assertThat(header(record, "event_id"))
                .isEqualTo(eventId.toString());

        assertThat(header(record, "request_id"))
                .isEqualTo("req-001");

        assertThat(header(record, "traceparent"))
                .startsWith("00-");

        assertThat(header(record, "tracestate"))
                .isEqualTo("vendor=value");
    }

    @Test
    void shouldAlwaysAddEventIdWhenStoredHeadersAreNull() {
        UUID eventId = UUID.randomUUID();

        OutboxMessage message = new OutboxMessage(
                eventId,
                "PAYMENT",
                UUID.randomUUID(),
                "payment.created",
                "{}",
                null,
                Instant.now(),
                0
        );

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        "payment.events.v1",
                        "key",
                        "{}"
                );

        mapper.apply(record, message);

        assertThat(header(record, "event_id"))
                .isEqualTo(eventId.toString());

        assertThat(header(record, "request_id"))
                .isNull();
    }

    private String header(
            ProducerRecord<String, String> record,
            String name
    ) {
        var header = record.headers().lastHeader(name);

        if (header == null) {
            return null;
        }

        return new String(
                header.value(),
                StandardCharsets.UTF_8
        );
    }
}