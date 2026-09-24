package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.refund.RefundStatus;
import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.infrastructure.persistence.entity.RefundJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.RefundPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.RefundJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundRepositoryAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-24T15:00:00Z"
            );

    private RefundJpaRepository repository;
    private ClockPort clockPort;

    private RefundRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository =
                mock(RefundJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(NOW);

        adapter =
                new RefundRepositoryAdapter(
                        repository,
                        new RefundPersistenceMapper(),
                        clockPort
                );
    }

    @Test
    void shouldFindRefundById() {
        Refund refund =
                requestedRefund();

        RefundJpaEntity entity =
                new RefundPersistenceMapper()
                        .toNewEntity(
                                refund,
                                LocalDateTime.of(
                                        2026,
                                        9,
                                        24,
                                        10,
                                        0
                                )
                        );

        when(
                repository.findById(
                        refund.id().value()
                )
        ).thenReturn(
                Optional.of(entity)
        );

        Optional<Refund> result =
                adapter.findById(
                        refund.id()
                );

        assertTrue(
                result.isPresent()
        );

        assertEquals(
                refund.id(),
                result.get().id()
        );

        assertEquals(
                refund.status(),
                result.get().status()
        );

        assertTrue(
                result.get()
                        .domainEvents()
                        .isEmpty()
        );
    }

    @Test
    void shouldUsePessimisticLookupForUpdate() {
        Refund refund =
                requestedRefund();

        RefundJpaEntity entity =
                new RefundPersistenceMapper()
                        .toNewEntity(
                                refund,
                                LocalDateTime.now()
                        );

        when(
                repository.findByIdForUpdate(
                        refund.id().value()
                )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.findByIdForUpdate(
                refund.id()
        );

        verify(repository)
                .findByIdForUpdate(
                        refund.id().value()
                );
    }

    @Test
    void shouldSumOnlyPendingRefundStatuses() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        when(
                repository.sumAmountByPaymentIdAndStatuses(
                        paymentId.value(),
                        List.of(
                                "REQUESTED",
                                "PROCESSING"
                        )
                )
        ).thenReturn(
                75_000L
        );

        Money result =
                adapter.sumPendingRefundAmount(
                        paymentId
                );

        assertEquals(
                Money.vnd(75_000),
                result
        );

        verify(repository)
                .sumAmountByPaymentIdAndStatuses(
                        paymentId.value(),
                        List.of(
                                "REQUESTED",
                                "PROCESSING"
                        )
                );
    }

    @Test
    void shouldReturnZeroWhenNoPendingRefundExists() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        when(
                repository.sumAmountByPaymentIdAndStatuses(
                        any(),
                        any()
                )
        ).thenReturn(0L);

        assertEquals(
                Money.vnd(0),
                adapter.sumPendingRefundAmount(
                        paymentId
                )
        );
    }

    @Test
    void shouldInsertRequestedRefund() {
        Refund refund =
                requestedRefund();

        when(
                repository.findById(
                        refund.id().value()
                )
        ).thenReturn(
                Optional.empty()
        );

        adapter.save(refund);

        verify(repository)
                .save(
                        argThat(entity ->
                                entity.getId()
                                        .equals(
                                                refund.id().value()
                                        )
                                        && entity.getStatus()
                                        .equals("REQUESTED")
                                        && entity.getCompletedAt()
                                        == null
                        )
                );
    }

    @Test
    void shouldSetCompletedAtWhenRefundBecomesSuccessful() {
        Refund refund =
                requestedRefund();

        RefundPersistenceMapper mapper =
                new RefundPersistenceMapper();

        RefundJpaEntity existing =
                mapper.toNewEntity(
                        refund,
                        LocalDateTime.of(
                                2026,
                                9,
                                24,
                                10,
                                0
                        )
                );

        existing.setVersion(4L);

        when(
                repository.findById(
                        refund.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        refund.markSucceeded();

        adapter.save(refund);

        assertEquals(
                "SUCCESS",
                existing.getStatus()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        15,
                        0
                ),
                existing.getCompletedAt()
        );

        assertEquals(
                4L,
                existing.getVersion()
        );
    }

    @Test
    void shouldNotReplaceExistingCompletedAt() {
        Refund refund =
                Refund.rehydrate(
                        new RefundId(UUID.randomUUID()),
                        new PaymentId(UUID.randomUUID()),
                        Money.vnd(50_000),
                        "refund",
                        new IdempotencyKey("refund-1"),
                        RefundStatus.SUCCESS,
                        null
                );

        RefundJpaEntity existing =
                new RefundPersistenceMapper()
                        .toNewEntity(
                                refund,
                                LocalDateTime.of(
                                        2026,
                                        9,
                                        20,
                                        10,
                                        0
                                )
                        );

        LocalDateTime completedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        20,
                        11,
                        0
                );

        existing.setCompletedAt(
                completedAt
        );

        when(
                repository.findById(
                        refund.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        adapter.save(refund);

        assertEquals(
                completedAt,
                existing.getCompletedAt()
        );
    }

    private Refund requestedRefund() {
        Refund refund =
                Refund.request(
                        new RefundId(UUID.randomUUID()),
                        new PaymentId(UUID.randomUUID()),
                        Money.vnd(50_000),
                        "Buyer requested refund",
                        new IdempotencyKey(
                                "refund-idem-001"
                        )
                );

        refund.clearDomainEvents();

        return refund;
    }
}