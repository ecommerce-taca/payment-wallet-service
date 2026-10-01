package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
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

class KafkaOutboxMessagePublisherAdapterTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate =
            mock(KafkaTemplate.class);

    private final KafkaTopicRouter topicRouter =
            new KafkaTopicRouter(
                    new KafkaTopicProperties(
                            "payment.events.v1",
                            "wallet.events.v1",
                            "payment-wallet.outbox.dlq.v1"
                    )
            );

    private final KafkaHeaderMapper headerMapper =
            new KafkaHeaderMapper(
                    new ObjectMapper()
            );

    private final KafkaOutboxMessagePublisherAdapter adapter =
            new KafkaOutboxMessagePublisherAdapter(
                    kafkaTemplate,
                    topicRouter,
                    headerMapper
            );

    @Test
    void shouldPublishPaymentCreatedUsingAggregateIdAsKey()
            throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        OutboxMessage message = new OutboxMessage(
                eventId,
                "PAYMENT",
                aggregateId,
                "payment.created",
                "{\"status\":\"PENDING\"}",
                null,
                Instant.parse("2026-10-01T10:00:00Z"),
                0
        );

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(message);

        ArgumentCaptor<ProducerRecord<String, String>> captor =
                producerRecordCaptor();

        verify(kafkaTemplate).send(
                captor.capture()
        );

        ProducerRecord<String, String> record =
                captor.getValue();

        assertThat(record.topic())
                .isEqualTo("payment.events.v1");

        assertThat(record.key())
                .isEqualTo(aggregateId.toString());

        assertThat(record.value())
                .isEqualTo(message.payload());

        assertThat(
                headerValue(
                        record,
                        KafkaHeaderNames.EVENT_ID
                )
        ).isEqualTo(eventId.toString());
    }

    @Test
    void shouldPublishWalletAllocatedToWalletTopic()
            throws Exception {

        OutboxMessage message =
                message(
                        "WALLET",
                        "wallet.allocated",
                        null
                );

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(message);

        ArgumentCaptor<ProducerRecord<String, String>> captor =
                producerRecordCaptor();

        verify(kafkaTemplate).send(
                captor.capture()
        );

        ProducerRecord<String, String> record =
                captor.getValue();

        assertThat(record.topic())
                .isEqualTo("wallet.events.v1");

        assertThat(record.key())
                .isEqualTo(
                        message.aggregateId()
                                .toString()
                );

        assertThat(record.value())
                .isEqualTo(message.payload());
    }

    @Test
    void shouldPropagateRequestAndTraceHeaders()
            throws Exception {

        UUID eventId = UUID.randomUUID();

        String headers = """
                {
                  "eventId":"%s",
                  "requestId":"req-001",
                  "traceparent":"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                  "tracestate":"vendor=value"
                }
                """.formatted(eventId);

        OutboxMessage message =
                new OutboxMessage(
                        eventId,
                        "PAYMENT",
                        UUID.randomUUID(),
                        "payment.created",
                        "{}",
                        headers,
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        ),
                        0
                );

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(message);

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
        ).isEqualTo("req-001");

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
        ).isEqualTo("vendor=value");
    }

    @Test
    void shouldPublishEventIdWhenStoredHeadersAreNull()
            throws Exception {

        OutboxMessage message =
                message(
                        "PAYMENT",
                        "payment.created",
                        null
                );

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(message);

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
        ).isEqualTo(
                message.eventId()
                        .toString()
        );

        assertThat(
                headerValue(
                        record,
                        KafkaHeaderNames.REQUEST_ID
                )
        ).isNull();

        assertThat(
                headerValue(
                        record,
                        KafkaHeaderNames.TRACEPARENT
                )
        ).isNull();

        assertThat(
                headerValue(
                        record,
                        KafkaHeaderNames.TRACESTATE
                )
        ).isNull();
    }

    @Test
    void shouldReportSupportedEventTypes() {
        assertThat(
                adapter.supportedEventTypes()
        ).containsExactlyInAnyOrder(
                "payment.created",
                "wallet.allocated"
        );
    }

    @Test
    void shouldRejectUnsupportedEvent() {
        OutboxMessage message =
                message(
                        "REFUND",
                        "refund.requested",
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.publish(message)
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldWrapKafkaFailure() {
        OutboxMessage message =
                message(
                        "PAYMENT",
                        "payment.created",
                        null
                );

        CompletableFuture<SendResult<String, String>> future =
                new CompletableFuture<>();

        future.completeExceptionally(
                new RuntimeException(
                        "Kafka unavailable"
                )
        );

        when(
                kafkaTemplate.send(
                        any(ProducerRecord.class)
                )
        ).thenReturn(future);

        assertThrows(
                KafkaOutboxPublishException.class,
                () -> adapter.publish(message)
        );
    }

    private OutboxMessage message(
            String aggregateType,
            String eventType,
            String headers
    ) {
        return new OutboxMessage(
                UUID.randomUUID(),
                aggregateType,
                UUID.randomUUID(),
                eventType,
                "{}",
                headers,
                Instant.parse(
                        "2026-10-01T10:00:00Z"
                ),
                0
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