package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxPublishingPersistenceAdapterTest {

    private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
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
        entity.setEventType("payment.succeeded");
        entity.setPayload("{\"amount\":100000}");
        entity.setHeaders(null);
        entity.setOccurredAt(LocalDateTime.of(2026, 9, 30, 10, 0));
        entity.setRetryCount(1);

        when(repository.lockNextBatch(10, 3)).thenReturn(List.of(entity));

        List<OutboxMessage> result = adapter.lockNextBatch(10, 3);

        assertThat(result).hasSize(1);

        OutboxMessage message = result.getFirst();
        assertThat(message.eventId()).isEqualTo(eventId);
        assertThat(message.aggregateId()).isEqualTo(aggregateId);
        assertThat(message.eventType()).isEqualTo("payment.succeeded");
        assertThat(message.retryCount()).isEqualTo(1);
        assertThat(message.occurredAt()).isEqualTo(Instant.parse("2026-09-30T10:00:00Z"));
    }

    @Test
    void shouldMarkEventPublished() {
        UUID eventId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-09-30T11:00:00Z");

        adapter.markPublished(eventId, publishedAt);

        verify(repository).markPublished(
                eventId,
                LocalDateTime.of(2026, 9, 30, 11, 0)
        );
    }

    @Test
    void shouldIncrementRetryAndTruncateLongError() {
        UUID eventId = UUID.randomUUID();
        String longError = "x".repeat(1500);

        adapter.recordFailure(eventId, longError);

        verify(repository).incrementFailure(
                eq(eventId),
                argThat(error -> error.length() == 1000)
        );
    }

    @Test
    void shouldRejectInvalidBatchSize() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> adapter.lockNextBatch(0, 3)
        );
    }
}