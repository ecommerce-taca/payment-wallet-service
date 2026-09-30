package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.InboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface InboxEventJpaRepository extends JpaRepository<InboxEventJpaEntity, UUID> {

    Optional<InboxEventJpaEntity> findByConsumerNameAndSourceAndEventId(
            String consumerName,
            String source,
            String eventId
    );

    @Modifying
    @Query(
            value = """
                    INSERT IGNORE INTO inbox_events (
                        id,
                        consumer_name,
                        source,
                        event_id,
                        event_type,
                        payload_hash,
                        received_at,
                        processed_at,
                        status,
                        failure_code
                    )
                    VALUES (
                        UNHEX(REPLACE(:id, '-', '')),
                        :consumerName,
                        :source,
                        :eventId,
                        :eventType,
                        :payloadHash,
                        :receivedAt,
                        NULL,
                        :status,
                        NULL
                    )
                    """,
            nativeQuery = true
    )
    int insertIgnoreInboxEvent(
            @Param("id") String id,
            @Param("consumerName") String consumerName,
            @Param("source") String source,
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("payloadHash") String payloadHash,
            @Param("receivedAt") LocalDateTime receivedAt,
            @Param("status") String status
    );
}
