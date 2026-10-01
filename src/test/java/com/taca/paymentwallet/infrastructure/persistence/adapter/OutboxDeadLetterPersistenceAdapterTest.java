package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
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

class OutboxDeadLetterPersistenceAdapterTest {

    private static final Set<String> EVENT_TYPES =
            Set.of("payment.created", "wallet.allocated");

    private final OutboxEventJpaRepository repository =
            mock(OutboxEventJpaRepository.class);

    private final OutboxDeadLetterPersistenceAdapter adapter =
            new OutboxDeadLetterPersistenceAdapter(repository);

    @Test
    void shouldMapLockedEntityToDeadLetter() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(eventId);
        entity.setAggregateType("PAYMENT");
        entity.setAggregateId(aggregateId);
        entity.setEventType("payment.created");
        entity.setPayload("{\"status\":\"PENDING\"}");
        entity.setHeaders(null);
        entity.setOccurredAt(LocalDateTime.of(2026, 10, 1, 10, 0));
        entity.setPublishedAt(null);
        entity.setRetryCount(3);
        entity.setNextAttemptAt(LocalDateTime.of(2026, 10, 1, 10, 0, 2));
        entity.setDeadLetteredAt(null);
        entity.setLastError("Kafka unavailable");

        when(repository.lockNextDeadLetterBatch(
                10,
                3,
                EVENT_TYPES
        )).thenReturn(List.of(entity));

        List<OutboxDeadLetter> result =
                adapter.lockNextDeadLetterBatch(
                        10,
                        3,
                        EVENT_TYPES
                );

        assertThat(result).hasSize(1);

        OutboxDeadLetter deadLetter = result.getFirst();

        assertThat(deadLetter.eventId()).isEqualTo(eventId);
        assertThat(deadLetter.aggregateId()).isEqualTo(aggregateId);
        assertThat(deadLetter.eventType()).isEqualTo("payment.created");
        assertThat(deadLetter.retryCount()).isEqualTo(3);
        assertThat(deadLetter.lastError()).isEqualTo("Kafka unavailable");
        assertThat(deadLetter.occurredAt())
                .isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
    }

    @Test
    void shouldMarkEventDeadLettered() {
        UUID eventId = UUID.randomUUID();
        Instant deadLetteredAt =
                Instant.parse("2026-10-01T10:05:00Z");

        adapter.markDeadLettered(eventId, deadLetteredAt);

        verify(repository).markDeadLettered(
                eventId,
                LocalDateTime.of(2026, 10, 1, 10, 5)
        );
    }

    @Test
    void shouldReturnEmptyWhenEventTypesAreEmpty() {
        List<OutboxDeadLetter> result =
                adapter.lockNextDeadLetterBatch(
                        10,
                        3,
                        Set.of()
                );

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectInvalidBatchSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.lockNextDeadLetterBatch(
                        0,
                        3,
                        EVENT_TYPES
                )
        );
    }

    @Test
    void shouldRejectInvalidMaxRetries() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.lockNextDeadLetterBatch(
                        10,
                        0,
                        EVENT_TYPES
                )
        );
    }
}