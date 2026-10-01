package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import com.taca.paymentwallet.application.port.out.OutboxDeadLetterPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class OutboxDeadLetterPersistenceAdapter implements OutboxDeadLetterPort {

    private final OutboxEventJpaRepository repository;

    public OutboxDeadLetterPersistenceAdapter(OutboxEventJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public List<OutboxDeadLetter> lockNextDeadLetterBatch(
            int batchSize,
            int maxRetries,
            Set<String> eventTypes
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }

        if (maxRetries <= 0) {
            throw new IllegalArgumentException("maxRetries must be positive");
        }

        Objects.requireNonNull(eventTypes, "eventTypes must not be null");

        if (eventTypes.isEmpty()) {
            return List.of();
        }

        return repository.lockNextDeadLetterBatch(
                batchSize,
                maxRetries,
                eventTypes
        ).stream().map(this::toDeadLetter).toList();
    }

    @Override
    public void markDeadLettered(UUID eventId, Instant deadLetteredAt) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(deadLetteredAt, "deadLetteredAt must not be null");

        repository.markDeadLettered(
                eventId,
                PersistenceTimeMapper.toLocalDateTime(deadLetteredAt)
        );
    }

    private OutboxDeadLetter toDeadLetter(OutboxEventJpaEntity entity) {
        return new OutboxDeadLetter(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getHeaders(),
                PersistenceTimeMapper.toInstant(entity.getOccurredAt()),
                entity.getRetryCount(),
                entity.getLastError()
        );
    }
}