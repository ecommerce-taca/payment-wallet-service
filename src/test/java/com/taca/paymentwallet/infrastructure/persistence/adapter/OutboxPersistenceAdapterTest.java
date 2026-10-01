package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.domain.payment.PaymentCreatedEvent;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentStatus;
import com.taca.paymentwallet.domain.payment.PaymentSucceededEvent;
import com.taca.paymentwallet.domain.payout.PayoutFailedEvent;
import com.taca.paymentwallet.domain.payout.PayoutStatus;
import com.taca.paymentwallet.domain.refund.RefundRequestedEvent;
import com.taca.paymentwallet.domain.settlement.SettlementBatchCompletedEvent;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.domain.wallet.WalletAllocatedEvent;
import com.taca.paymentwallet.application.metadata.RequestMetadata;
import com.taca.paymentwallet.application.metadata.RequestMetadataContext;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

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

        try {
            when(
                    objectMapper.writeValueAsString(
                            any(
                                    com.taca.paymentwallet.infrastructure.messaging.metadata.OutboxHeaders.class
                            )
                    )
            ).thenReturn(
                    "{}"
            );
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        adapter =
                new OutboxPersistenceAdapter(
                        repository,
                        objectMapper
                );
    }

    @AfterEach
    void clearMetadata() {
        RequestMetadataContext.clear();
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

        when(
                objectMapper.writeValueAsString(
                        any(com.taca.paymentwallet.infrastructure.messaging.metadata.OutboxHeaders.class)
                )
        ).thenReturn(
                """
                {"eventId":"01991e80-1111-7000-8000-000000000001"}
                """
        );

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

        assertNotNull(entity.getHeaders());

        assertTrue(
                entity.getHeaders().contains(
                        EVENT_ID.toString()
                )
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
                        new PayoutId(
                                payoutId
                        ),
                        new ShopId(
                                UUID.randomUUID()
                        ),
                        Money.vnd(
                                100_000
                        ),
                        PayoutStatus.FAILED,
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

    @Test
    void shouldPersistPaymentCreatedEvent()
            throws Exception {

        UUID paymentId = UUID.randomUUID();

        PaymentCreatedEvent event =
                new PaymentCreatedEvent(
                        UUID.randomUUID(),
                        OCCURRED_AT,
                        new PaymentId(paymentId),
                        new CheckoutGroupId(UUID.randomUUID()),
                        List.of(
                                new OrderId(
                                        UUID.randomUUID()
                                )
                        ),
                        Money.vnd(
                                100_000
                        ),
                        PaymentMethod.VNPAY,
                        PaymentStatus.PENDING
                );

        when(
                objectMapper.writeValueAsString(
                        event
                )
        ).thenReturn(
                "{}"
        );

        adapter.save(
                event
        );

        verify(
                repository
        ).save(
                argThat(entity ->
                        "PAYMENT".equals(
                                entity.getAggregateType()
                        )
                                && paymentId.equals(
                                entity.getAggregateId()
                        )
                                && "payment.created".equals(
                                entity.getEventType()
                        )
                )
        );
    }

    @Test
    void shouldMapWalletAggregateType() throws Exception {

        UUID walletId = UUID.randomUUID();

        WalletAllocatedEvent event = new WalletAllocatedEvent(
                UUID.randomUUID(),
                OCCURRED_AT,
                new WalletId(walletId),
                new OrderId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(3_000),
                Money.vnd(90_000)
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

        verify(repository).save(captor.capture());

        OutboxEventJpaEntity entity = captor.getValue();

        assertEquals(
                "WALLET",
                entity.getAggregateType()
        );

        assertEquals(
                walletId,
                entity.getAggregateId()
        );

        assertEquals(
                "wallet.allocated",
                entity.getEventType()
        );
    }

    @Test
    void shouldSerializePayoutFailedEventWithAmountAndStatus() {
        ObjectMapper realObjectMapper =
                new ObjectMapper();

        OutboxPersistenceAdapter realAdapter =
                new OutboxPersistenceAdapter(
                        repository,
                        realObjectMapper
                );

        PayoutFailedEvent event =
                new PayoutFailedEvent(
                        EVENT_ID,
                        OCCURRED_AT,
                        new PayoutId(
                                UUID.randomUUID()
                        ),
                        new ShopId(
                                UUID.randomUUID()
                        ),
                        Money.vnd(
                                100_000
                        ),
                        PayoutStatus.FAILED,
                        "BANK_TRANSFER_FAILED"
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

        assertTrue(
                payload.contains(
                        "100000"
                )
        );

        assertTrue(
                payload.contains(
                        "FAILED"
                )
        );

        assertTrue(
                payload.contains(
                        "BANK_TRANSFER_FAILED"
                )
        );
    }

    @Test
    void shouldPersistRequestAndTraceMetadata()
            throws Exception {

        PaymentSucceededEvent event =
                new PaymentSucceededEvent(
                        EVENT_ID,
                        OCCURRED_AT,
                        new PaymentId(PAYMENT_ID),
                        Money.vnd(100_000)
                );

        RequestMetadataContext.set(
                new RequestMetadata(
                        "req-001",
                        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                        "vendor=value"
                )
        );

        when(
                objectMapper.writeValueAsString(event)
        ).thenReturn("{}");

        when(
                objectMapper.writeValueAsString(
                        any(com.taca.paymentwallet.infrastructure.messaging.metadata.OutboxHeaders.class)
                )
        ).thenReturn(
                """
                {
                  "eventId":"01991e80-1111-7000-8000-000000000001",
                  "requestId":"req-001",
                  "traceparent":"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                  "tracestate":"vendor=value"
                }
                """
        );

        adapter.save(event);

        ArgumentCaptor<OutboxEventJpaEntity> captor =
                ArgumentCaptor.forClass(
                        OutboxEventJpaEntity.class
                );

        verify(repository).save(captor.capture());

        String headers = captor.getValue().getHeaders();

        assertTrue(headers.contains("req-001"));
        assertTrue(headers.contains("traceparent"));
        assertTrue(headers.contains("vendor=value"));
    }
}