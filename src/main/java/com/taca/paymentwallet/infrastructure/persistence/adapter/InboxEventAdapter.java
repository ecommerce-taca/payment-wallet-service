package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.InboxEventNotFoundException;
import com.taca.paymentwallet.application.inbox.InboxEvent;
import com.taca.paymentwallet.application.inbox.InboxEventKey;
import com.taca.paymentwallet.application.inbox.InboxEventStatus;
import com.taca.paymentwallet.application.port.out.InboxEventPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.InboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.InboxEventJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.time.Instant;
import java.util.Objects;

public class InboxEventAdapter
        implements InboxEventPort {

    private final InboxEventJpaRepository repository;
    private final PersistenceUuidGenerator uuidGenerator;

    public InboxEventAdapter(
            InboxEventJpaRepository repository,
            PersistenceUuidGenerator uuidGenerator
    ) {
        this.repository =
                Objects.requireNonNull(repository);

        this.uuidGenerator =
                Objects.requireNonNull(uuidGenerator);
    }

    @Override
    public boolean recordIfAbsent(
            InboxEvent event
    ) {
        Objects.requireNonNull(
                event,
                "event must not be null"
        );

        InboxEventKey key =
                event.key();

        int inserted =
                repository.insertIgnoreInboxEvent(
                        uuidGenerator
                                .next(event.receivedAt())
                                .toString(),

                        key.consumerName(),
                        key.source(),
                        key.eventId(),

                        event.eventType(),
                        event.payloadHash(),

                        PersistenceTimeMapper
                                .toLocalDateTime(
                                        event.receivedAt()
                                ),

                        event.status().name()
                );

        return inserted == 1;
    }

    @Override
    public void markProcessed(
            InboxEventKey key,
            Instant processedAt
    ) {
        Objects.requireNonNull(
                key,
                "key must not be null"
        );

        Objects.requireNonNull(
                processedAt,
                "processedAt must not be null"
        );

        InboxEventJpaEntity entity =
                repository
                        .findByConsumerNameAndSourceAndEventId(
                                key.consumerName(),
                                key.source(),
                                key.eventId()
                        )
                        .orElseThrow(
                                () ->
                                        new InboxEventNotFoundException(
                                                key
                                        )
                        );

        if (InboxEventStatus.PROCESSED.name()
                .equals(entity.getStatus())) {
            return;
        }

        entity.setStatus(
                InboxEventStatus.PROCESSED.name()
        );

        entity.setProcessedAt(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                processedAt
                        )
        );

        entity.setFailureCode(null);

        repository.save(entity);
    }
}