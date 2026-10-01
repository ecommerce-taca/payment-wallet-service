package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KafkaDeadLetterPublisherAdapterTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate =
            mock(KafkaTemplate.class);

    private final KafkaTopicProperties properties =
            new KafkaTopicProperties(
                    "payment.events.v1",
                    "wallet.events.v1",
                    "payment-wallet.outbox.dlq.v1"
            );

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final KafkaDeadLetterPublisherAdapter adapter =
            new KafkaDeadLetterPublisherAdapter(
                    kafkaTemplate,
                    properties,
                    objectMapper
            );

    @Test
    void shouldPublishDeadLetterToDlqUsingAggregateIdAsKey() {
        OutboxDeadLetter deadLetter = deadLetter();

        when(kafkaTemplate.send(
                eq("payment-wallet.outbox.dlq.v1"),
                eq(deadLetter.aggregateId().toString()),
                anyString()
        )).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(deadLetter);

        verify(kafkaTemplate).send(
                eq("payment-wallet.outbox.dlq.v1"),
                eq(deadLetter.aggregateId().toString()),
                argThat(payload ->
                        payload.contains(deadLetter.eventId().toString())
                                && payload.contains("payment.created")
                                && payload.contains("Kafka unavailable")
                )
        );
    }

    @Test
    void shouldIncludeRetryCountInDlqPayload() {
        OutboxDeadLetter deadLetter = deadLetter();

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        adapter.publish(deadLetter);

        verify(kafkaTemplate).send(
                anyString(),
                anyString(),
                argThat(payload -> payload.contains("\"retryCount\":3"))
        );
    }

    @Test
    void shouldWrapKafkaFailure() {
        OutboxDeadLetter deadLetter = deadLetter();

        CompletableFuture<SendResult<String, String>> future =
                new CompletableFuture<>();

        future.completeExceptionally(
                new RuntimeException("Kafka unavailable")
        );

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(future);

        assertThrows(
                KafkaOutboxPublishException.class,
                () -> adapter.publish(deadLetter)
        );
    }

    @Test
    void shouldRestoreInterruptFlagWhenInterrupted() {
        OutboxDeadLetter deadLetter = deadLetter();

        CompletableFuture<SendResult<String, String>> future =
                mock(CompletableFuture.class);

        try {
            when(future.get()).thenThrow(new InterruptedException());

            when(kafkaTemplate.send(
                    anyString(),
                    anyString(),
                    anyString()
            )).thenReturn(future);

            assertThrows(
                    KafkaOutboxPublishException.class,
                    () -> adapter.publish(deadLetter)
            );

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        } finally {
            Thread.interrupted();
        }
    }

    private OutboxDeadLetter deadLetter() {
        return new OutboxDeadLetter(
                UUID.randomUUID(),
                "PAYMENT",
                UUID.randomUUID(),
                "payment.created",
                "{\"status\":\"PENDING\"}",
                null,
                Instant.parse("2026-10-01T10:00:00Z"),
                3,
                "Kafka unavailable"
        );
    }
}