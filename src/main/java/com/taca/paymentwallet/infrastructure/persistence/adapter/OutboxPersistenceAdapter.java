package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.OutboxPort;
import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class OutboxPersistenceAdapter
        implements OutboxPort {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxPersistenceAdapter(
            OutboxEventJpaRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository =
                Objects.requireNonNull(repository);

        this.objectMapper =
                Objects.requireNonNull(objectMapper);
    }

    @Override
    public void save(
            DomainEvent event
    ) {
        Objects.requireNonNull(
                event,
                "event must not be null"
        );

        OutboxEventJpaEntity entity =
                new OutboxEventJpaEntity();

        /*
         * Domain event đã có identity riêng.
         * Không sinh thêm persistence UUID.
         */
        entity.setId(
                event.eventId()
        );

        entity.setAggregateType(
                resolveAggregateType(event)
        );

        entity.setAggregateId(
                parseAggregateId(event)
        );

        entity.setEventType(
                event.eventType()
        );

        entity.setPayload(
                serialize(event)
        );

        /*
         * Headers hiện Application/Domain chưa cung cấp.
         * Không tự bịa traceId/correlationId.
         */
        entity.setHeaders(null);

        entity.setOccurredAt(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                event.occurredAt()
                        )
        );

        entity.setPublishedAt(null);
        entity.setRetryCount(0);
        entity.setLastError(null);

        repository.save(entity);
    }

    private String serialize(
            DomainEvent event
    ) {
        try {
            return objectMapper
                    .writeValueAsString(event);
        } catch (Exception exception) {
            throw new OutboxSerializationException(
                    event.eventType(),
                    exception
            );
        }
    }

    private UUID parseAggregateId(
            DomainEvent event
    ) {
        try {
            return UUID.fromString(
                    event.aggregateId()
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "aggregateId must be a UUID for outbox event "
                            + event.eventType()
                            + ": "
                            + event.aggregateId(),
                    exception
            );
        }
    }

    private String resolveAggregateType(
            DomainEvent event
    ) {
        String eventType =
                Objects.requireNonNull(
                                event.eventType(),
                                "eventType must not be null"
                        )
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (eventType.startsWith("payment.")) {
            return "PAYMENT";
        }

        if (eventType.startsWith("refund.")) {
            return "REFUND";
        }

        if (eventType.startsWith("payout.")) {
            return "PAYOUT";
        }

        if (eventType.startsWith("settlement.")) {
            return "SETTLEMENT_BATCH";
        }

        throw new IllegalArgumentException(
                "unsupported outbox event type: "
                        + event.eventType()
        );
    }
}