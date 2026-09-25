package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.IdempotencyRecordNotFoundException;
import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;
import com.taca.paymentwallet.application.idempotency.IdempotencyStatus;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdempotencyPersistenceAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-25T05:00:00Z"
            );

    private static final Duration TTL =
            Duration.ofHours(24);

    private IdempotencyKeyJpaRepository repository;
    private ClockPort clockPort;

    private IdempotencyPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        repository =
                mock(IdempotencyKeyJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(NOW);

        adapter =
                new IdempotencyPersistenceAdapter(
                        repository,
                        clockPort,
                        new PersistenceUuidGenerator(),
                        TTL
                );
    }

    @Test
    void shouldReserveNewIdempotencyRecord() {
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(
                        UUID.randomUUID()
                );

        IdempotencyScope scope =
                IdempotencyScope.payment(
                        checkoutGroupId
                );

        when(
                repository
                        .findByScopeAndScopeIdAndIdempotencyKey(
                                "PAYMENT",
                                checkoutGroupId
                                        .value()
                                        .toString(),
                                "idem-001"
                        )
        ).thenReturn(
                Optional.empty()
        );

        IdempotencyRecord result =
                adapter.reserve(
                        scope,
                        "idem-001",
                        "request-hash-001"
                );

        assertEquals(
                IdempotencyStatus.PROCESSING,
                result.status()
        );

        assertEquals(
                "request-hash-001",
                result.requestHash()
        );

        assertNull(
                result.responsePayload()
        );

        assertNull(
                result.failureCode()
        );

        ArgumentCaptor<IdempotencyKeyJpaEntity>
                captor =
                ArgumentCaptor.forClass(
                        IdempotencyKeyJpaEntity.class
                );

        verify(repository)
                .save(
                        captor.capture()
                );

        IdempotencyKeyJpaEntity entity =
                captor.getValue();

        assertEquals(
                "PAYMENT",
                entity.getScope()
        );

        assertEquals(
                checkoutGroupId
                        .value()
                        .toString(),
                entity.getScopeId()
        );

        assertEquals(
                "PROCESSING",
                entity.getStatus()
        );

        assertEquals(
                7,
                entity.getId().version()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        5,
                        0
                ),
                entity.getCreatedAt()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        26,
                        5,
                        0
                ),
                entity.getExpiresAt()
        );
    }

    @Test
    void shouldFindActiveIdempotencyRecord() {
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(
                        UUID.randomUUID()
                );

        IdempotencyScope scope =
                IdempotencyScope.payment(
                        checkoutGroupId
                );

        IdempotencyKeyJpaEntity entity =
                entity(
                        "PAYMENT",
                        checkoutGroupId
                                .value()
                                .toString(),
                        "idem-001"
                );

        when(
                repository
                        .findByScopeAndScopeIdAndIdempotencyKey(
                                "PAYMENT",
                                checkoutGroupId
                                        .value()
                                        .toString(),
                                "idem-001"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        Optional<IdempotencyRecord> result =
                adapter.find(
                        scope,
                        "idem-001"
                );

        assertTrue(
                result.isPresent()
        );

        assertEquals(
                scope,
                result.get().scope()
        );

        assertEquals(
                IdempotencyStatus.PROCESSING,
                result.get().status()
        );
    }

    @Test
    void shouldIgnoreExpiredRecordOnFind() {
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(
                        UUID.randomUUID()
                );

        IdempotencyScope scope =
                IdempotencyScope.payment(
                        checkoutGroupId
                );

        IdempotencyKeyJpaEntity entity =
                entity(
                        "PAYMENT",
                        checkoutGroupId
                                .value()
                                .toString(),
                        "idem-001"
                );

        entity.setExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        4,
                        59
                )
        );

        when(
                repository
                        .findByScopeAndScopeIdAndIdempotencyKey(
                                "PAYMENT",
                                checkoutGroupId
                                        .value()
                                        .toString(),
                                "idem-001"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        assertTrue(
                adapter.find(
                        scope,
                        "idem-001"
                ).isEmpty()
        );

        verify(repository, never())
                .delete(any());
    }

    @Test
    void shouldReuseExpiredRecordOnReserve() {
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(
                        UUID.randomUUID()
                );

        IdempotencyScope scope =
                IdempotencyScope.payment(
                        checkoutGroupId
                );

        IdempotencyKeyJpaEntity entity =
                entity(
                        "PAYMENT",
                        checkoutGroupId
                                .value()
                                .toString(),
                        "idem-001"
                );

        UUID existingId =
                entity.getId();

        entity.setStatus("FAILED");
        entity.setFailureCode("OLD_FAILURE");

        entity.setExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        4,
                        0
                )
        );

        when(
                repository
                        .findByScopeAndScopeIdAndIdempotencyKey(
                                "PAYMENT",
                                checkoutGroupId
                                        .value()
                                        .toString(),
                                "idem-001"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.reserve(
                scope,
                "idem-001",
                "new-request-hash"
        );

        assertEquals(
                existingId,
                entity.getId()
        );

        assertEquals(
                "PROCESSING",
                entity.getStatus()
        );

        assertEquals(
                "new-request-hash",
                entity.getRequestHash()
        );

        assertNull(
                entity.getFailureCode()
        );

        assertNull(
                entity.getResponseSnapshot()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        26,
                        5,
                        0
                ),
                entity.getExpiresAt()
        );

        verify(repository)
                .save(entity);
    }

    @Test
    void shouldMarkRecordSucceeded() {
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(
                        UUID.randomUUID()
                );

        IdempotencyScope scope =
                IdempotencyScope.payment(
                        checkoutGroupId
                );

        IdempotencyKeyJpaEntity entity =
                entity(
                        "PAYMENT",
                        checkoutGroupId
                                .value()
                                .toString(),
                        "idem-001"
                );

        when(
                repository
                        .findByScopeAndScopeIdAndIdempotencyKey(
                                "PAYMENT",
                                checkoutGroupId
                                        .value()
                                        .toString(),
                                "idem-001"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.markSucceeded(
                scope,
                "idem-001",
                """
                {"paymentId":"123"}
                """
        );

        assertEquals(
                "SUCCEEDED",
                entity.getStatus()
        );

        assertNotNull(
                entity.getResponseSnapshot()
        );

        assertNull(
                entity.getFailureCode()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        5,
                        0
                ),
                entity.getUpdatedAt()
        );

        verify(repository)
                .save(entity);
    }

    @Test
    void shouldMarkRecordFailed() {
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(
                        UUID.randomUUID()
                );

        IdempotencyScope scope =
                IdempotencyScope.payment(
                        checkoutGroupId
                );

        IdempotencyKeyJpaEntity entity =
                entity(
                        "PAYMENT",
                        checkoutGroupId
                                .value()
                                .toString(),
                        "idem-001"
                );

        when(
                repository
                        .findByScopeAndScopeIdAndIdempotencyKey(
                                "PAYMENT",
                                checkoutGroupId
                                        .value()
                                        .toString(),
                                "idem-001"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.markFailed(
                scope,
                "idem-001",
                "PAYMENT_CREATION_FAILED"
        );

        assertEquals(
                "FAILED",
                entity.getStatus()
        );

        assertEquals(
                "PAYMENT_CREATION_FAILED",
                entity.getFailureCode()
        );

        assertNull(
                entity.getResponseSnapshot()
        );

        verify(repository)
                .save(entity);
    }

    @Test
    void shouldRejectMarkSucceededForUnknownRecord() {
        CheckoutGroupId checkoutGroupId =
                new CheckoutGroupId(
                        UUID.randomUUID()
                );

        IdempotencyScope scope =
                IdempotencyScope.payment(
                        checkoutGroupId
                );

        when(
                repository
                        .findByScopeAndScopeIdAndIdempotencyKey(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                IdempotencyRecordNotFoundException.class,
                () ->
                        adapter.markSucceeded(
                                scope,
                                "idem-001",
                                "{}"
                        )
        );
    }

    @Test
    void shouldRejectNonPositiveTtl() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new IdempotencyPersistenceAdapter(
                                repository,
                                clockPort,
                                new PersistenceUuidGenerator(),
                                Duration.ZERO
                        )
        );
    }

    private IdempotencyKeyJpaEntity entity(
            String scope,
            String scopeId,
            String key
    ) {
        IdempotencyKeyJpaEntity entity =
                new IdempotencyKeyJpaEntity();

        entity.setId(
                UUID.randomUUID()
        );

        entity.setScope(scope);
        entity.setScopeId(scopeId);
        entity.setIdempotencyKey(key);
        entity.setRequestHash("request-hash");
        entity.setStatus("PROCESSING");

        entity.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        4,
                        0
                )
        );

        entity.setUpdatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        4,
                        0
                )
        );

        entity.setExpiresAt(
                LocalDateTime.of(
                        2026,
                        9,
                        26,
                        4,
                        0
                )
        );

        return entity;
    }
}