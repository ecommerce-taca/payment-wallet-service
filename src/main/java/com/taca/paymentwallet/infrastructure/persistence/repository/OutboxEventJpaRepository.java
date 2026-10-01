package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    List<OutboxEventJpaEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE published_at IS NULL
              AND retry_count < :maxRetries
            ORDER BY occurred_at ASC, id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> lockNextBatch(
            @Param("batchSize") int batchSize,
            @Param("maxRetries") int maxRetries
    );

    @Modifying
    @Query("""
            update OutboxEventJpaEntity e
            set e.publishedAt = :publishedAt,
                e.nextAttemptAt = null,
                e.lastError = null
            where e.id = :eventId
              and e.publishedAt is null
            """)
    int markPublished(
            @Param("eventId") UUID eventId,
            @Param("publishedAt") LocalDateTime publishedAt
    );

    @Modifying
    @Query("""
            update OutboxEventJpaEntity e
            set e.retryCount = e.retryCount + 1,
                e.nextAttemptAt = :nextAttemptAt,
                e.lastError = :lastError
            where e.id = :eventId
              and e.publishedAt is null
            """)
    int incrementFailure(
            @Param("eventId") UUID eventId,
            @Param("lastError") String lastError,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt
    );

    @Query(value = """
        SELECT *
        FROM outbox_events
        WHERE published_at IS NULL
          AND retry_count < :maxRetries
          AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
          AND event_type IN (:eventTypes)
        ORDER BY occurred_at ASC, id ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEventJpaEntity> lockNextBatch(
            @Param("batchSize") int batchSize,
            @Param("maxRetries") int maxRetries,
            @Param("now") LocalDateTime now,
            @Param("eventTypes") Set<String> eventTypes
    );

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE published_at IS NULL
              AND dead_lettered_at IS NULL
              AND retry_count >= :maxRetries
              AND event_type IN (:eventTypes)
            ORDER BY occurred_at ASC, id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> lockNextDeadLetterBatch(
            @Param("batchSize") int batchSize,
            @Param("maxRetries") int maxRetries,
            @Param("eventTypes") Set<String> eventTypes
    );

    @Modifying
    @Query("""
        update OutboxEventJpaEntity e
        set e.deadLetteredAt = :deadLetteredAt,
            e.nextAttemptAt = null
        where e.id = :eventId
          and e.publishedAt is null
          and e.deadLetteredAt is null
        """)
    int markDeadLettered(
            @Param("eventId") UUID eventId,
            @Param("deadLetteredAt") LocalDateTime deadLetteredAt
    );
}