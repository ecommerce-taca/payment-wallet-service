package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.domain.payment.PaymentSucceededEvent;
import com.taca.paymentwallet.domain.payout.PayoutFailedEvent;
import com.taca.paymentwallet.domain.refund.RefundRequestedEvent;
import com.taca.paymentwallet.domain.settlement.SettlementBatchCompletedEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxPersistenceAdapterTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "01991e80-1111-7000-8000-000000000001"
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "40000000-0000-0000-0000-000000000001"
            );

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-09-25T06:30:00Z"
            );

    private OutboxEventJpaRepository repository;
    private ObjectMapper objectMapper;

    private OutboxPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        repository =
                mock(OutboxEventJpaRepository.class);

        objectMapper =
                mock(ObjectMapper.class);

        adapter =
                new OutboxPersistenceAdapter(
                        repository,
                        objectMapper
                );
    }

    @Test
    void shouldPersistPaymentDomainEvent() throws Exception {
        PaymentSucceededEvent event =
                new PaymentSucceededEvent(
                        EVENT_ID,
                        OCCURRED_AT,
                        new PaymentId(PAYMENT_ID),
                        Money.vnd(100_000)
                );

        String payload =
                """
                {"paymentId":"40000000-0000-0000-0000-000000000001"}
                """;

        when(
                objectMapper.writeValueAsString(event)
        ).thenReturn(payload);

        adapter.save(event);

        ArgumentCaptor<OutboxEventJpaEntity>
                captor =
                ArgumentCaptor.forClass(
                        OutboxEventJpaEntity.class
                );

        verify(repository)
                .save(
                        captor.capture()
                );

        OutboxEventJpaEntity entity =
                captor.getValue();

        assertEquals(
                EVENT_ID,
                entity.getId()
        );

        assertEquals(
                "PAYMENT",
                entity.getAggregateType()
        );

        assertEquals(
                PAYMENT_ID,
                entity.getAggregateId()
        );

        assertEquals(
                "payment.succeeded",
                entity.getEventType()
        );

        assertEquals(
                payload,
                entity.getPayload()
        );

        assertNull(
                entity.getHeaders()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        6,
                        30
                ),
                entity.getOccurredAt()
        );

        assertNull(
                entity.getPublishedAt()
        );

        assertEquals(
                0,
                entity.getRetryCount()
        );

        assertNull(
                entity.getLastError()
        );
    }

    @Test
    void shouldMapRefundAggregateType() throws Exception {
        UUID refundId =
                UUID.randomUUID();

        RefundRequestedEvent event =
                new RefundRequestedEvent(
                        UUID.randomUUID(),
                        OCCURRED_AT,
                        new RefundId(refundId),
                        new PaymentId(
                                UUID.randomUUID()
                        ),
                        Money.vnd(50_000)
                );

        when(
                objectMapper.writeValueAsString(event)
        ).thenReturn("{}");

        adapter.save(event);

        ArgumentCaptor<OutboxEventJpaEntity>
                captor =
                ArgumentCaptor.forClass(
                        OutboxEventJpaEntity.class
                );

        verify(repository)
                .save(
                        captor.capture()
                );

        assertEquals(
                "REFUND",
                captor.getValue()
                        .getAggregateType()
        );

        assertEquals(
                refundId,
                captor.getValue()
                        .getAggregateId()
        );
    }

    @Test
    void shouldMapPayoutAggregateType() throws Exception {
        UUID payoutId =
                UUID.randomUUID();

        PayoutFailedEvent event =
                new PayoutFailedEvent(
                        UUID.randomUUID(),
                        OCCURRED_AT,
                        new PayoutId(payoutId),
                        new ShopId(
                                UUID.randomUUID()
                        ),
                        "BANK_TRANSFER_FAILED"
                );

        when(
                objectMapper.writeValueAsString(event)
        ).thenReturn("{}");

        adapter.save(event);

        ArgumentCaptor<OutboxEventJpaEntity>
                captor =
                ArgumentCaptor.forClass(
                        OutboxEventJpaEntity.class
                );

        verify(repository)
                .save(
                        captor.capture()
                );

        assertEquals(
                "PAYOUT",
                captor.getValue()
                        .getAggregateType()
        );

        assertEquals(
                payoutId,
                captor.getValue()
                        .getAggregateId()
        );
    }

    @Test
    void shouldMapSettlementAggregateType() throws Exception {
        UUID batchId =
                UUID.randomUUID();

        SettlementBatchCompletedEvent event =
                new SettlementBatchCompletedEvent(
                        UUID.randomUUID(),
                        OCCURRED_AT,
                        new SettlementBatchId(
                                batchId
                        ),
                        Money.vnd(200_000)
                );

        when(
                objectMapper.writeValueAsString(event)
        ).thenReturn("{}");

        adapter.save(event);

        ArgumentCaptor<OutboxEventJpaEntity>
                captor =
                ArgumentCaptor.forClass(
                        OutboxEventJpaEntity.class
                );

        verify(repository)
                .save(
                        captor.capture()
                );

        assertEquals(
                "SETTLEMENT_BATCH",
                captor.getValue()
                        .getAggregateType()
        );

        assertEquals(
                batchId,
                captor.getValue()
                        .getAggregateId()
        );
    }

    @Test
    void shouldFailWhenSerializationFails()
            throws Exception {

        PaymentSucceededEvent event =
                new PaymentSucceededEvent(
                        EVENT_ID,
                        OCCURRED_AT,
                        new PaymentId(
                                PAYMENT_ID
                        ),
                        Money.vnd(100_000)
                );

        when(
                objectMapper.writeValueAsString(event)
        ).thenThrow(
                new RuntimeException(
                        "serialization failed"
                )
        );

        OutboxSerializationException exception =
                assertThrows(
                        OutboxSerializationException.class,
                        () ->
                                adapter.save(event)
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "payment.succeeded"
                        )
        );

        verifyNoInteractions(
                repository
        );
    }

    @Test
    void shouldUseDomainEventIdAsOutboxId()
            throws Exception {

        PaymentSucceededEvent event =
                new PaymentSucceededEvent(
                        EVENT_ID,
                        OCCURRED_AT,
                        new PaymentId(
                                PAYMENT_ID
                        ),
                        Money.vnd(100_000)
                );

        when(
                objectMapper.writeValueAsString(event)
        ).thenReturn("{}");

        adapter.save(event);

        verify(repository)
                .save(
                        argThat(entity ->
                                EVENT_ID.equals(
                                        entity.getId()
                                )
                        )
                );
    }

    @Test
    void shouldSerializeRealPaymentEvent() {
        ObjectMapper realObjectMapper =
                new ObjectMapper();

        OutboxPersistenceAdapter realAdapter =
                new OutboxPersistenceAdapter(
                        repository,
                        realObjectMapper
                );

        PaymentSucceededEvent event =
                new PaymentSucceededEvent(
                        EVENT_ID,
                        OCCURRED_AT,
                        new PaymentId(PAYMENT_ID),
                        Money.vnd(100_000)
                );

        realAdapter.save(event);

        ArgumentCaptor<OutboxEventJpaEntity>
                captor =
                ArgumentCaptor.forClass(
                        OutboxEventJpaEntity.class
                );

        verify(repository)
                .save(
                        captor.capture()
                );

        String payload =
                captor.getValue()
                        .getPayload();

        assertNotNull(payload);
        assertFalse(payload.isBlank());

        assertTrue(
                payload.contains(
                        PAYMENT_ID.toString()
                )
        );
    }
}