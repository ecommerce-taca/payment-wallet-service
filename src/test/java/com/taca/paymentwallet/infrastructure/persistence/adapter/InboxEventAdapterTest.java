package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.InboxEventNotFoundException;
import com.taca.paymentwallet.application.inbox.InboxEvent;
import com.taca.paymentwallet.application.inbox.InboxEventKey;
import com.taca.paymentwallet.application.inbox.InboxEventStatus;
import com.taca.paymentwallet.infrastructure.persistence.entity.InboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.InboxEventJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InboxEventAdapterTest {

    private static final Instant RECEIVED_AT =
            Instant.parse(
                    "2026-09-25T06:00:00Z"
            );

    private static final Instant PROCESSED_AT =
            Instant.parse(
                    "2026-09-25T06:01:00Z"
            );

    private InboxEventJpaRepository repository;
    private InboxEventAdapter adapter;

    @BeforeEach
    void setUp() {
        repository =
                mock(InboxEventJpaRepository.class);

        adapter =
                new InboxEventAdapter(
                        repository,
                        new PersistenceUuidGenerator()
                );
    }

    @Test
    void shouldRecordInboxEventWhenAbsent() {
        InboxEvent event =
                event();

        when(
                repository.insertIgnoreInboxEvent(
                        anyString(),
                        eq("payment-wallet-shipment-consumer"),
                        eq("shipment-service"),
                        eq("shipment-event-001"),
                        eq("shipment.delivered"),
                        eq(event.payloadHash()),
                        eq(
                                LocalDateTime.of(
                                        2026,
                                        9,
                                        25,
                                        6,
                                        0
                                )
                        ),
                        eq("RECEIVED")
                )
        ).thenReturn(1);

        assertTrue(
                adapter.recordIfAbsent(event)
        );
    }

    @Test
    void shouldReturnFalseForDuplicateInboxEvent() {
        InboxEvent event =
                event();

        when(
                repository.insertIgnoreInboxEvent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        anyString()
                )
        ).thenReturn(0);

        assertFalse(
                adapter.recordIfAbsent(event)
        );
    }

    @Test
    void shouldGenerateVersion7PersistenceId() {
        InboxEvent event =
                event();

        when(
                repository.insertIgnoreInboxEvent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        anyString()
                )
        ).thenReturn(1);

        adapter.recordIfAbsent(event);

        verify(repository)
                .insertIgnoreInboxEvent(
                        argThat(id ->
                                UUID.fromString(id)
                                        .version()
                                        == 7
                        ),
                        eq("payment-wallet-shipment-consumer"),
                        eq("shipment-service"),
                        eq("shipment-event-001"),
                        eq("shipment.delivered"),
                        eq(event.payloadHash()),
                        any(),
                        eq("RECEIVED")
                );
    }

    @Test
    void shouldMarkInboxEventProcessed() {
        InboxEventKey key =
                key();

        InboxEventJpaEntity entity =
                entity();

        when(
                repository
                        .findByConsumerNameAndSourceAndEventId(
                                key.consumerName(),
                                key.source(),
                                key.eventId()
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.markProcessed(
                key,
                PROCESSED_AT
        );

        assertEquals(
                "PROCESSED",
                entity.getStatus()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        6,
                        1
                ),
                entity.getProcessedAt()
        );

        assertNull(
                entity.getFailureCode()
        );

        verify(repository)
                .save(entity);
    }

    @Test
    void shouldNotRewriteAlreadyProcessedEvent() {
        InboxEventKey key =
                key();

        InboxEventJpaEntity entity =
                entity();

        LocalDateTime originalProcessedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        5,
                        55
                );

        entity.setStatus("PROCESSED");
        entity.setProcessedAt(
                originalProcessedAt
        );

        when(
                repository
                        .findByConsumerNameAndSourceAndEventId(
                                key.consumerName(),
                                key.source(),
                                key.eventId()
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.markProcessed(
                key,
                PROCESSED_AT
        );

        assertEquals(
                originalProcessedAt,
                entity.getProcessedAt()
        );

        verify(repository, never())
                .save(any());
    }

    @Test
    void shouldRejectUnknownInboxEventWhenMarkingProcessed() {
        InboxEventKey key =
                key();

        when(
                repository
                        .findByConsumerNameAndSourceAndEventId(
                                key.consumerName(),
                                key.source(),
                                key.eventId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                InboxEventNotFoundException.class,
                () ->
                        adapter.markProcessed(
                                key,
                                PROCESSED_AT
                        )
        );
    }

    @Test
    void shouldRejectNullProcessedAt() {
        assertThrows(
                NullPointerException.class,
                () ->
                        adapter.markProcessed(
                                key(),
                                null
                        )
        );
    }

    private InboxEvent event() {
        return new InboxEvent(
                key(),
                "shipment.delivered",
                "abcdef0123456789"
                        + "abcdef0123456789"
                        + "abcdef0123456789"
                        + "abcdef0123456789",
                RECEIVED_AT,
                InboxEventStatus.RECEIVED
        );
    }

    private InboxEventKey key() {
        return new InboxEventKey(
                "payment-wallet-shipment-consumer",
                "shipment-service",
                "shipment-event-001"
        );
    }

    private InboxEventJpaEntity entity() {
        InboxEventJpaEntity entity =
                new InboxEventJpaEntity();

        entity.setId(
                UUID.randomUUID()
        );

        entity.setConsumerName(
                "payment-wallet-shipment-consumer"
        );

        entity.setSource(
                "shipment-service"
        );

        entity.setEventId(
                "shipment-event-001"
        );

        entity.setEventType(
                "shipment.delivered"
        );

        entity.setPayloadHash(
                "abcdef0123456789"
                        + "abcdef0123456789"
                        + "abcdef0123456789"
                        + "abcdef0123456789"
        );

        entity.setReceivedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        6,
                        0
                )
        );

        entity.setStatus("RECEIVED");

        return entity;
    }
}