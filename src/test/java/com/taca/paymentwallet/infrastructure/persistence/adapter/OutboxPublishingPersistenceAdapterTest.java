package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OutboxPublishingPersistenceAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-10-01T00:00:00Z");

    private static final Set<String> SUPPORTED_EVENT_TYPES =
            Set.of("payment.created", "wallet.allocated");

    private final OutboxEventJpaRepository repository =
            mock(OutboxEventJpaRepository.class);

    private final OutboxPublishingPersistenceAdapter adapter =
            new OutboxPublishingPersistenceAdapter(repository);

    @Test
    void shouldMapLockedEntityToOutboxMessage() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(eventId);
        entity.setAggregateType("PAYMENT");
        entity.setAggregateId(aggregateId);
        entity.setEventType("payment.created");
        entity.setPayload("{\"status\":\"PENDING\"}");
        entity.setHeaders(null);
        entity.setOccurredAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        entity.setRetryCount(1);

        when(repository.lockNextBatch(
                10,
                3,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                SUPPORTED_EVENT_TYPES
        )).thenReturn(List.of(entity));

        List<OutboxMessage> result = adapter.lockNextBatch(
                10,
                3,
                NOW,
                SUPPORTED_EVENT_TYPES
        );

        assertThat(result).hasSize(1);

        OutboxMessage message = result.getFirst();

        assertThat(message.eventId()).isEqualTo(eventId);
        assertThat(message.aggregateId()).isEqualTo(aggregateId);
        assertThat(message.eventType()).isEqualTo("payment.created");
        assertThat(message.retryCount()).isEqualTo(1);
        assertThat(message.occurredAt())
                .isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    void shouldMarkEventPublished() {
        UUID eventId = UUID.randomUUID();
        Instant publishedAt =
                Instant.parse("2026-10-01T00:00:01Z");

        adapter.markPublished(eventId, publishedAt);

        verify(repository).markPublished(
                eventId,
                LocalDateTime.of(2026, 10, 1, 0, 0, 1)
        );
    }

    @Test
    void shouldIncrementRetryAndSetNextAttemptAt() {
        UUID eventId = UUID.randomUUID();
        String longError = "x".repeat(1500);
        Instant nextAttemptAt =
                Instant.parse("2026-10-01T00:00:02Z");

        adapter.recordFailure(
                eventId,
                longError,
                nextAttemptAt
        );

        verify(repository).incrementFailure(
                eq(eventId),
                argThat(error -> error.length() == 1000),
                eq(LocalDateTime.of(2026, 10, 1, 0, 0, 2))
        );
    }

    @Test
    void shouldReturnEmptyWhenSupportedEventTypesIsEmpty() {
        List<OutboxMessage> result = adapter.lockNextBatch(
                10,
                3,
                NOW,
                Set.of()
        );

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectInvalidBatchSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.lockNextBatch(
                        0,
                        3,
                        NOW,
                        SUPPORTED_EVENT_TYPES
                )
        );
    }

    @Test
    void shouldRejectInvalidMaxRetries() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.lockNextBatch(
                        10,
                        0,
                        NOW,
                        SUPPORTED_EVENT_TYPES
                )
        );
    }
}