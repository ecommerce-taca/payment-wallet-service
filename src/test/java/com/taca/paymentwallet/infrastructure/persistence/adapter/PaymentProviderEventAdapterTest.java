package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.PaymentProviderEventNotFoundException;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEvent;
import com.taca.paymentwallet.application.paymentevent.PaymentProviderEventStatus;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentEventJpaRepository;
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

class PaymentProviderEventAdapterTest {

    private static final Instant RECEIVED_AT =
            Instant.parse(
                    "2026-09-25T05:30:00Z"
            );

    private static final Instant APPLIED_AT =
            Instant.parse(
                    "2026-09-25T05:31:00Z"
            );

    private PaymentEventJpaRepository repository;
    private ClockPort clockPort;

    private PaymentProviderEventAdapter adapter;

    @BeforeEach
    void setUp() {
        repository =
                mock(PaymentEventJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(APPLIED_AT);

        adapter =
                new PaymentProviderEventAdapter(
                        repository,
                        clockPort,
                        new PersistenceUuidGenerator()
                );
    }

    @Test
    void shouldRecordProviderEventWhenAbsent() {
        PaymentProviderEvent event =
                event();

        when(
                repository.insertIgnoreProviderEvent(
                        anyString(),
                        eq(event.paymentId()
                                .value()
                                .toString()),
                        eq("VNPAY"),
                        eq("event-001"),
                        eq("txn-001"),
                        eq("00"),
                        eq("00"),
                        eq(100_000L),
                        eq("VND"),
                        eq(event.payloadHash()),
                        eq(
                                LocalDateTime.of(
                                        2026,
                                        9,
                                        25,
                                        5,
                                        30
                                )
                        ),
                        eq("RECEIVED")
                )
        ).thenReturn(1);

        boolean result =
                adapter.recordIfAbsent(event);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseForDuplicateProviderEvent() {
        PaymentProviderEvent event =
                event();

        when(
                repository.insertIgnoreProviderEvent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyLong(),
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
        PaymentProviderEvent event =
                event();

        when(
                repository.insertIgnoreProviderEvent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        any(),
                        anyString()
                )
        ).thenReturn(1);

        adapter.recordIfAbsent(event);

        verify(repository)
                .insertIgnoreProviderEvent(
                        argThat(id ->
                                UUID.fromString(id)
                                        .version()
                                        == 7
                        ),
                        eq(
                                event.paymentId()
                                        .value()
                                        .toString()
                        ),
                        eq("VNPAY"),
                        eq("event-001"),
                        eq("txn-001"),
                        eq("00"),
                        eq("00"),
                        eq(100_000L),
                        eq("VND"),
                        eq(event.payloadHash()),
                        any(),
                        eq("RECEIVED")
                );
    }

    @Test
    void shouldMarkProviderEventApplied() {
        PaymentEventJpaEntity entity =
                entity();

        when(
                repository
                        .findByProviderAndProviderEventId(
                                "VNPAY",
                                "event-001"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.markApplied(
                "vnpay",
                " event-001 "
        );

        assertEquals(
                "APPLIED",
                entity.getStatus()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        5,
                        31
                ),
                entity.getAppliedAt()
        );

        verify(repository)
                .save(entity);
    }

    @Test
    void shouldNotRewriteAlreadyAppliedEvent() {
        PaymentEventJpaEntity entity =
                entity();

        LocalDateTime originalAppliedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        5,
                        20
                );

        entity.setStatus("APPLIED");
        entity.setAppliedAt(
                originalAppliedAt
        );

        when(
                repository
                        .findByProviderAndProviderEventId(
                                "VNPAY",
                                "event-001"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.markApplied(
                "VNPAY",
                "event-001"
        );

        assertEquals(
                originalAppliedAt,
                entity.getAppliedAt()
        );

        verify(repository, never())
                .save(any());

        verifyNoInteractions(clockPort);
    }

    @Test
    void shouldRejectUnknownProviderEventWhenMarkingApplied() {
        when(
                repository
                        .findByProviderAndProviderEventId(
                                "VNPAY",
                                "missing-event"
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                PaymentProviderEventNotFoundException.class,
                () ->
                        adapter.markApplied(
                                "VNPAY",
                                "missing-event"
                        )
        );
    }

    private PaymentProviderEvent event() {
        return new PaymentProviderEvent(
                "VNPAY",
                "event-001",
                "txn-001",
                new PaymentId(
                        UUID.randomUUID()
                ),
                "00",
                "00",
                Money.vnd(100_000),
                "0123456789abcdef"
                        + "0123456789abcdef"
                        + "0123456789abcdef"
                        + "0123456789abcdef",
                RECEIVED_AT,
                PaymentProviderEventStatus.RECEIVED
        );
    }

    private PaymentEventJpaEntity entity() {
        PaymentEventJpaEntity entity =
                new PaymentEventJpaEntity();

        entity.setId(
                UUID.randomUUID()
        );

        entity.setPaymentId(
                UUID.randomUUID()
        );

        entity.setProvider("VNPAY");
        entity.setProviderEventId("event-001");
        entity.setProviderTransactionRef("txn-001");
        entity.setProviderResponseCode("00");
        entity.setProviderTransactionStatus("00");
        entity.setAmount(100_000L);
        entity.setCurrency("VND");
        entity.setPayloadHash(
                "0123456789abcdef"
                        + "0123456789abcdef"
                        + "0123456789abcdef"
                        + "0123456789abcdef"
        );

        entity.setReceivedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        5,
                        30
                )
        );

        entity.setStatus("RECEIVED");

        return entity;
    }
}