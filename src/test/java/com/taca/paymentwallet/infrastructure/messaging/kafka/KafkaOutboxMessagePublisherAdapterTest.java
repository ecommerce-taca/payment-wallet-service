package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class KafkaOutboxMessagePublisherAdapterTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate =
            mock(KafkaTemplate.class);

    private final KafkaTopicRouter router = new KafkaTopicRouter(
            new KafkaTopicProperties("payment.events.v1", "wallet.events.v1")
    );

    private final KafkaOutboxMessagePublisherAdapter adapter =
            new KafkaOutboxMessagePublisherAdapter(kafkaTemplate, router);

    @Test
    void shouldPublishPaymentCreatedUsingAggregateIdAsKey() {
        UUID aggregateId = UUID.randomUUID();

        OutboxMessage message = new OutboxMessage(
                UUID.randomUUID(),
                "PAYMENT",
                aggregateId,
                "payment.created",
                "{\"status\":\"PENDING\"}",
                null,
                Instant.parse("2026-09-30T10:00:00Z"),
                0
        );

        when(kafkaTemplate.send(
                "payment.events.v1",
                aggregateId.toString(),
                message.payload()
        )).thenReturn(CompletableFuture.completedFuture(null));

        adapter.publish(message);

        verify(kafkaTemplate).send(
                "payment.events.v1",
                aggregateId.toString(),
                message.payload()
        );
    }

    @Test
    void shouldPublishWalletAllocatedToWalletTopic() {
        OutboxMessage message = message("WALLET", "wallet.allocated");

        when(kafkaTemplate.send(
                "wallet.events.v1",
                message.aggregateId().toString(),
                message.payload()
        )).thenReturn(CompletableFuture.completedFuture(null));

        adapter.publish(message);

        verify(kafkaTemplate).send(
                "wallet.events.v1",
                message.aggregateId().toString(),
                message.payload()
        );
    }

    @Test
    void shouldReportSupportedEvent() {
        assertThat(adapter.supports(message("PAYMENT", "payment.created"))).isTrue();
        assertThat(adapter.supports(message("PAYMENT", "payment.succeeded"))).isFalse();
    }

    @Test
    void shouldRejectUnsupportedEvent() {
        OutboxMessage message = message("REFUND", "refund.requested");

        assertThrows(IllegalArgumentException.class, () -> adapter.publish(message));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldWrapKafkaFailure() {
        OutboxMessage message = message("PAYMENT", "payment.created");

        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka unavailable"));

        when(kafkaTemplate.send(
                "payment.events.v1",
                message.aggregateId().toString(),
                message.payload()
        )).thenReturn(future);

        assertThrows(KafkaOutboxPublishException.class, () -> adapter.publish(message));
    }

    @Test
    void shouldExposeSupportedEventTypes() {
        assertThat(adapter.supportedEventTypes())
                .containsExactlyInAnyOrder(
                        "payment.created",
                        "wallet.allocated"
                );
    }

    private OutboxMessage message(String aggregateType, String eventType) {
        return new OutboxMessage(
                UUID.randomUUID(),
                aggregateType,
                UUID.randomUUID(),
                eventType,
                "{}",
                null,
                Instant.parse("2026-09-30T10:00:00Z"),
                0
        );
    }
}