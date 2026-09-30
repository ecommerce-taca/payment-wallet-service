package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.payment.PaymentAttempt;
import com.taca.paymentwallet.domain.payment.PaymentAttemptStatus;
import com.taca.paymentwallet.domain.valueobject.PaymentAttemptId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAttemptJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAttemptJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentAttemptRepositoryAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-27T06:00:00Z"
            );

    private static final Instant EXPIRES_AT =
            Instant.parse(
                    "2026-09-27T06:15:00Z"
            );

    private PaymentAttemptJpaRepository repository;
    private ClockPort clockPort;

    private PaymentAttemptRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository =
                mock(
                        PaymentAttemptJpaRepository.class
                );

        clockPort =
                mock(
                        ClockPort.class
                );

        when(
                clockPort.now()
        ).thenReturn(
                NOW
        );

        adapter =
                new PaymentAttemptRepositoryAdapter(
                        repository,
                        new PaymentAttemptPersistenceMapper(),
                        clockPort
                );
    }

    @Test
    void shouldInsertNewPaymentAttempt() {
        PaymentAttempt attempt =
                attempt();

        when(
                repository.findById(
                        attempt.id().value()
                )
        ).thenReturn(
                Optional.empty()
        );

        PaymentAttempt result =
                adapter.save(
                        attempt
                );

        assertSame(
                attempt,
                result
        );

        ArgumentCaptor<PaymentAttemptJpaEntity> captor =
                ArgumentCaptor.forClass(
                        PaymentAttemptJpaEntity.class
                );

        verify(
                repository
        ).save(
                captor.capture()
        );

        PaymentAttemptJpaEntity entity =
                captor.getValue();

        assertEquals(
                attempt.id().value(),
                entity.getId()
        );

        assertEquals(
                attempt.paymentId().value(),
                entity.getPaymentId()
        );

        assertEquals(
                "VNPAY",
                entity.getProvider()
        );

        assertEquals(
                "txn-ref-001",
                entity.getProviderTransactionRef()
        );

        assertEquals(
                "PENDING",
                entity.getStatus()
        );

        assertEquals(
                attempt.requestHash(),
                entity.getRequestHash()
        );

        assertEquals(
                attempt.paymentUrlHash(),
                entity.getPaymentUrlHash()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        27,
                        6,
                        15
                ),
                entity.getExpiresAt()
        );

        assertNull(
                entity.getCompletedAt()
        );

        assertNull(
                entity.getFailureCode()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        27,
                        6,
                        0
                ),
                entity.getCreatedAt()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        27,
                        6,
                        0
                ),
                entity.getUpdatedAt()
        );
    }

    @Test
    void shouldUpdateExistingPaymentAttempt() {
        PaymentAttempt attempt =
                attempt();

        Instant completedAt =
                Instant.parse(
                        "2026-09-27T06:05:00Z"
                );

        attempt.markSucceeded(
                completedAt
        );

        PaymentAttemptJpaEntity existing =
                entity();

        when(
                repository.findById(
                        attempt.id().value()
                )
        ).thenReturn(
                Optional.of(
                        existing
                )
        );

        PaymentAttempt result =
                adapter.save(
                        attempt
                );

        assertSame(
                attempt,
                result
        );

        assertEquals(
                "SUCCESS",
                existing.getStatus()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        27,
                        6,
                        5
                ),
                existing.getCompletedAt()
        );

        assertNull(
                existing.getFailureCode()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        27,
                        6,
                        0
                ),
                existing.getUpdatedAt()
        );

        verify(
                repository
        ).save(
                existing
        );
    }

    @Test
    void shouldFindByProviderAndProviderTransactionRef() {
        PaymentAttemptJpaEntity entity =
                entity();

        when(
                repository
                        .findByProviderAndProviderTransactionRef(
                                "VNPAY",
                                "txn-ref-001"
                        )
        ).thenReturn(
                Optional.of(
                        entity
                )
        );

        Optional<PaymentAttempt> result =
                adapter
                        .findByProviderAndProviderTransactionRef(
                                " vnpay ",
                                " txn-ref-001 "
                        );

        assertTrue(
                result.isPresent()
        );

        PaymentAttempt attempt =
                result.get();

        assertEquals(
                entity.getId(),
                attempt.id().value()
        );

        assertEquals(
                entity.getPaymentId(),
                attempt.paymentId().value()
        );

        assertEquals(
                "VNPAY",
                attempt.provider()
        );

        assertEquals(
                "txn-ref-001",
                attempt.providerTransactionRef()
        );

        assertEquals(
                PaymentAttemptStatus.PENDING,
                attempt.status()
        );

        assertEquals(
                entity.getRequestHash(),
                attempt.requestHash()
        );

        assertEquals(
                entity.getPaymentUrlHash(),
                attempt.paymentUrlHash()
        );

        verify(
                repository
        ).findByProviderAndProviderTransactionRef(
                "VNPAY",
                "txn-ref-001"
        );
    }

    @Test
    void shouldReturnEmptyWhenProviderTransactionRefNotFound() {
        when(
                repository
                        .findByProviderAndProviderTransactionRef(
                                "VNPAY",
                                "missing-ref"
                        )
        ).thenReturn(
                Optional.empty()
        );

        Optional<PaymentAttempt> result =
                adapter
                        .findByProviderAndProviderTransactionRef(
                                "VNPAY",
                                "missing-ref"
                        );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldFindAttemptsByPaymentId() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.randomUUID()
                );

        PaymentAttemptJpaEntity first =
                entity();

        first.setPaymentId(
                paymentId.value()
        );

        PaymentAttemptJpaEntity second =
                entity();

        second.setId(
                UUID.randomUUID()
        );

        second.setPaymentId(
                paymentId.value()
        );

        second.setProviderTransactionRef(
                "txn-ref-002"
        );

        when(
                repository
                        .findByPaymentIdOrderByCreatedAtDesc(
                                paymentId.value()
                        )
        ).thenReturn(
                List.of(
                        first,
                        second
                )
        );

        List<PaymentAttempt> result =
                adapter.findByPaymentId(
                        paymentId
                );

        assertEquals(
                2,
                result.size()
        );

        assertEquals(
                "txn-ref-001",
                result.get(0)
                        .providerTransactionRef()
        );

        assertEquals(
                "txn-ref-002",
                result.get(1)
                        .providerTransactionRef()
        );

        verify(
                repository
        ).findByPaymentIdOrderByCreatedAtDesc(
                paymentId.value()
        );
    }

    @Test
    void shouldReturnEmptyListWhenPaymentHasNoAttempts() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.randomUUID()
                );

        when(
                repository
                        .findByPaymentIdOrderByCreatedAtDesc(
                                paymentId.value()
                        )
        ).thenReturn(
                List.of()
        );

        List<PaymentAttempt> result =
                adapter.findByPaymentId(
                        paymentId
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldReturnTrueWhenPaymentHasPendingAttempt() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.randomUUID()
                );

        when(
                repository
                        .existsByPaymentIdAndStatus(
                                paymentId.value(),
                                "PENDING"
                        )
        ).thenReturn(
                true
        );

        boolean result =
                adapter.existsPendingByPaymentId(
                        paymentId
                );

        assertTrue(
                result
        );

        verify(
                repository
        ).existsByPaymentIdAndStatus(
                paymentId.value(),
                "PENDING"
        );
    }

    @Test
    void shouldReturnFalseWhenPaymentHasNoPendingAttempt() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.randomUUID()
                );

        when(
                repository
                        .existsByPaymentIdAndStatus(
                                paymentId.value(),
                                "PENDING"
                        )
        ).thenReturn(
                false
        );

        assertFalse(
                adapter.existsPendingByPaymentId(
                        paymentId
                )
        );
    }

    @Test
    void shouldRejectNullAttemptWhenSaving() {
        assertThrows(
                NullPointerException.class,
                () ->
                        adapter.save(
                                null
                        )
        );

        verifyNoInteractions(
                repository
        );
    }

    @Test
    void shouldRejectNullPaymentIdWhenFindingAttempts() {
        assertThrows(
                NullPointerException.class,
                () ->
                        adapter.findByPaymentId(
                                null
                        )
        );

        verifyNoInteractions(
                repository
        );
    }

    @Test
    void shouldRejectNullPaymentIdWhenCheckingPendingAttempt() {
        assertThrows(
                NullPointerException.class,
                () ->
                        adapter.existsPendingByPaymentId(
                                null
                        )
        );

        verifyNoInteractions(
                repository
        );
    }

    @Test
    void shouldRejectBlankProviderWhenFindingByTransactionRef() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adapter
                                .findByProviderAndProviderTransactionRef(
                                        " ",
                                        "txn-ref-001"
                                )
        );

        verifyNoInteractions(
                repository
        );
    }

    @Test
    void shouldRejectBlankTransactionRefWhenFindingByProvider() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adapter
                                .findByProviderAndProviderTransactionRef(
                                        "VNPAY",
                                        " "
                                )
        );

        verifyNoInteractions(
                repository
        );
    }

    private PaymentAttempt attempt() {
        return PaymentAttempt.create(
                new PaymentAttemptId(
                        UUID.randomUUID()
                ),
                new PaymentId(
                        UUID.randomUUID()
                ),
                "vnpay",
                "txn-ref-001",
                hash(
                        'a'
                ),
                hash(
                        'b'
                ),
                EXPIRES_AT
        );
    }

    private PaymentAttemptJpaEntity entity() {
        PaymentAttemptJpaEntity entity =
                new PaymentAttemptJpaEntity();

        entity.setId(
                UUID.randomUUID()
        );

        entity.setPaymentId(
                UUID.randomUUID()
        );

        entity.setProvider(
                "VNPAY"
        );

        entity.setProviderTransactionRef(
                "txn-ref-001"
        );

        entity.setStatus(
                "PENDING"
        );

        entity.setRequestHash(
                hash(
                        'a'
                )
        );

        entity.setPaymentUrlHash(
                hash(
                        'b'
                )
        );

        entity.setExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        27,
                        6,
                        15
                )
        );

        entity.setCompletedAt(
                null
        );

        entity.setFailureCode(
                null
        );

        entity.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        27,
                        5,
                        59
                )
        );

        entity.setUpdatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        27,
                        5,
                        59
                )
        );

        return entity;
    }

    private String hash(
            char value
    ) {
        return String.valueOf(
                value
        ).repeat(
                64
        );
    }
}