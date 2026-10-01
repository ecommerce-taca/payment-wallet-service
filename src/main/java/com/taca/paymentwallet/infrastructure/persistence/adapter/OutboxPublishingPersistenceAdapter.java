package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.application.port.out.OutboxPublishingPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class OutboxPublishingPersistenceAdapter implements OutboxPublishingPort {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final OutboxEventJpaRepository repository;

    public OutboxPublishingPersistenceAdapter(OutboxEventJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public List<OutboxMessage> lockNextBatch(
            int batchSize,
            int maxRetries,
            Instant now,
            Set<String> eventTypes
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }

        if (maxRetries <= 0) {
            throw new IllegalArgumentException("maxRetries must be positive");
        }

        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(eventTypes, "eventTypes must not be null");

        if (eventTypes.isEmpty()) {
            return List.of();
        }

        return repository.lockNextBatch(
                batchSize,
                maxRetries,
                PersistenceTimeMapper.toLocalDateTime(now),
                eventTypes
        ).stream().map(this::toMessage).toList();
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");

        repository.markPublished(
                eventId,
                PersistenceTimeMapper.toLocalDateTime(publishedAt)
        );
    }

    @Override
    public void recordFailure(
            UUID eventId,
            String error,
            Instant nextAttemptAt
    ) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");

        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("error must not be blank");
        }

        repository.incrementFailure(
                eventId,
                truncate(error),
                PersistenceTimeMapper.toLocalDateTime(nextAttemptAt)
        );
    }

    private OutboxMessage toMessage(OutboxEventJpaEntity entity) {
        return new OutboxMessage(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getHeaders(),
                PersistenceTimeMapper.toInstant(entity.getOccurredAt()),
                entity.getRetryCount()
        );
    }

    private String truncate(String value) {
        return value.length() <= MAX_ERROR_LENGTH
                ? value
                : value.substring(0, MAX_ERROR_LENGTH);
    }
}