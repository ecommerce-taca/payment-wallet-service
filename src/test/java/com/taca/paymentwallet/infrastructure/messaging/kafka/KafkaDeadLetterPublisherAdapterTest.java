package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import com.taca.paymentwallet.application.outbox.OutboxMessage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaDeadLetterPublisherAdapterTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate =
            mock(KafkaTemplate.class);

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final SimpleMeterRegistry meterRegistry =
            new SimpleMeterRegistry();

    private final OutboxPublisherObservation observation =
            new OutboxPublisherObservation(
                    meterRegistry
            );

    private final KafkaTopicProperties properties =
            new KafkaTopicProperties(
                    "payment.events.v1",
                    "wallet.events.v1",
                    "shipment.events.v1",
                    "payment-wallet.outbox.dlq.v1"
            );

    private final KafkaDeadLetterHeaderMapper headerMapper =
            new KafkaDeadLetterHeaderMapper(
                    objectMapper
            );

    private final KafkaDeadLetterPublisherAdapter adapter =
            new KafkaDeadLetterPublisherAdapter(
                    kafkaTemplate,
                    properties,
                    objectMapper,
                    headerMapper,
                    observation
            );

    @Test
    void shouldPublishToDlqUsingAggregateIdAsKey()
            throws Exception {

        OutboxDeadLetter deadLetter =
                deadLetter(null);

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(deadLetter);

        ArgumentCaptor<ProducerRecord<String, String>> captor =
                producerRecordCaptor();

        verify(kafkaTemplate).send(
                captor.capture()
        );

        ProducerRecord<String, String> record =
                captor.getValue();

        assertThat(record.topic())
                .isEqualTo(
                        "payment-wallet.outbox.dlq.v1"
                );

        assertThat(record.key())
                .isEqualTo(
                        deadLetter.aggregateId()
                                .toString()
                );

        assertThat(record.value())
                .contains(
                        deadLetter.eventId()
                                .toString()
                );

        assertThat(record.value())
                .contains("payment.created");

        assertThat(record.value())
                .contains("Kafka unavailable");

        assertThat(record.value())
                .contains("\"retryCount\":3");
    }

    @Test
    void shouldPropagateOriginalHeadersToDlq()
            throws Exception {

        UUID eventId = UUID.randomUUID();

        String headers = """
                {
                  "eventId":"%s",
                  "requestId":"req-dlq-001",
                  "traceparent":"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                  "tracestate":"vendor=value"
                }
                """.formatted(eventId);

        OutboxDeadLetter deadLetter =
                new OutboxDeadLetter(
                        eventId,
                        "PAYMENT",
                        UUID.randomUUID(),
                        "payment.created",
                        "{}",
                        headers,
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        ),
                        3,
                        "Kafka unavailable"
                );

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(deadLetter);

        ArgumentCaptor<ProducerRecord<String, String>> captor =
                producerRecordCaptor();

        verify(kafkaTemplate).send(
                captor.capture()
        );

        ProducerRecord<String, String> record =
                captor.getValue();

        assertThat(
                headerValue(
                        record,
                        KafkaHeaderNames.EVENT_ID
                )
        ).isEqualTo(eventId.toString());

        assertThat(
                headerValue(
                        record,
                        KafkaHeaderNames.REQUEST_ID
                )
        ).isEqualTo(
                "req-dlq-001"
        );

        assertThat(
                headerValue(
                        record,
                        KafkaHeaderNames.TRACEPARENT
                )
        ).isEqualTo(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );

        assertThat(
                headerValue(
                        record,
                        KafkaHeaderNames.TRACESTATE
                )
        ).isEqualTo(
                "vendor=value"
        );
    }

    @Test
    void shouldAlwaysPublishEventIdHeader()
            throws Exception {

        OutboxDeadLetter deadLetter =
                deadLetter(null);

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(deadLetter);

        ArgumentCaptor<ProducerRecord<String, String>> captor =
                producerRecordCaptor();

        verify(kafkaTemplate).send(
                captor.capture()
        );

        assertThat(
                headerValue(
                        captor.getValue(),
                        KafkaHeaderNames.EVENT_ID
                )
        ).isEqualTo(
                deadLetter.eventId()
                        .toString()
        );
    }

    @Test
    void shouldWrapKafkaFailure() {
        OutboxDeadLetter deadLetter =
                deadLetter(null);

        CompletableFuture<SendResult<String, String>> future =
                new CompletableFuture<>();

        future.completeExceptionally(
                new RuntimeException(
                        "DLQ unavailable"
                )
        );

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(future);

        assertThrows(
                KafkaOutboxPublishException.class,
                () -> adapter.publish(deadLetter)
        );
    }

    @Test
    void shouldRecordSuccessfulDlqPublishMetric()
            throws Exception {

        OutboxDeadLetter deadLetter =
                deadLetter(null);

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(deadLetter);

        double count = meterRegistry
                .get(
                        OutboxPublisherObservation
                                .DLQ_TOTAL
                )
                .tag(
                        "event_type",
                        "payment.created"
                )
                .tag(
                        "topic",
                        "payment-wallet.outbox.dlq.v1"
                )
                .tag(
                        "result",
                        "success"
                )
                .counter()
                .count();

        assertThat(count)
                .isEqualTo(1.0);
    }

    @Test
    void shouldRecordDlqPublishLatency()
            throws Exception {

        OutboxDeadLetter deadLetter =
                deadLetter(null);

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(deadLetter);

        var timer = meterRegistry
                .get(
                        OutboxPublisherObservation.PUBLISH_LATENCY
                )
                .tag(
                        "event_type",
                        "payment.created"
                )
                .tag(
                        "topic",
                        "payment-wallet.outbox.dlq.v1"
                )
                .tag(
                        "result",
                        "success"
                )
                .timer();

        assertThat(timer.count())
                .isEqualTo(1);
    }

    private OutboxDeadLetter deadLetter(
            String headers
    ) {
        return new OutboxDeadLetter(
                UUID.randomUUID(),
                "PAYMENT",
                UUID.randomUUID(),
                "payment.created",
                "{\"status\":\"PENDING\"}",
                headers,
                Instant.parse(
                        "2026-10-01T10:00:00Z"
                ),
                3,
                "Kafka unavailable"
        );
    }

    @SuppressWarnings({
            "unchecked",
            "rawtypes"
    })
    private ArgumentCaptor<ProducerRecord<String, String>>
    producerRecordCaptor() {
        return ArgumentCaptor.forClass(
                (Class) ProducerRecord.class
        );
    }

    private String headerValue(
            ProducerRecord<String, String> record,
            String name
    ) {
        var header =
                record.headers()
                        .lastHeader(name);

        if (header == null) {
            return null;
        }

        return new String(
                header.value(),
                StandardCharsets.UTF_8
        );
    }
}