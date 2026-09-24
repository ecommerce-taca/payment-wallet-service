package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.payout.BankAccountSnapshot;
import com.taca.paymentwallet.domain.payout.Payout;
import com.taca.paymentwallet.domain.payout.PayoutStatus;
import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PayoutJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.BankAccountSnapshotPersistenceCodec;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PayoutPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PayoutJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PayoutRepositoryAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-09-24T16:00:00Z");

    private PayoutJpaRepository repository;
    private ClockPort clockPort;
    private PayoutPersistenceMapper mapper;

    private PayoutRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(PayoutJpaRepository.class);

        clockPort = mock(ClockPort.class);

        when(clockPort.now()).thenReturn(NOW);

        mapper =
                new PayoutPersistenceMapper(
                        new BankAccountSnapshotPersistenceCodec()
                );

        adapter =
                new PayoutRepositoryAdapter(
                        repository,
                        mapper,
                        clockPort
                );
    }

    @Test
    void shouldFindPayoutById() {
        Payout payout = requestedPayout();

        PayoutJpaEntity entity =
                mapper.toNewEntity(
                        payout,
                        LocalDateTime.of(
                                2026,
                                9,
                                24,
                                10,
                                0
                        )
                );

        when(repository.findById(payout.id().value())
        ).thenReturn(Optional.of(entity));

        Optional<Payout> result =
                adapter.findById(
                        payout.id()
                );

        assertTrue(result.isPresent());

        assertEquals(
                payout.id(),
                result.get().id()
        );

        assertEquals(
                payout.walletId(),
                result.get().walletId()
        );

        assertEquals(
                payout.shopId(),
                result.get().shopId()
        );

        assertEquals(
                payout.amount(),
                result.get().amount()
        );

        assertEquals(
                payout.bankAccountSnapshot(),
                result.get().bankAccountSnapshot()
        );

        assertEquals(
                payout.idempotencyKey(),
                result.get().idempotencyKey()
        );

        assertEquals(
                payout.status(),
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
        Payout payout =
                requestedPayout();

        PayoutJpaEntity entity =
                mapper.toNewEntity(
                        payout,
                        LocalDateTime.now()
                );

        when(
                repository.findByIdForUpdate(
                        payout.id().value()
                )
        ).thenReturn(
                Optional.of(entity)
        );

        adapter.findByIdForUpdate(
                payout.id()
        );

        verify(repository)
                .findByIdForUpdate(
                        payout.id().value()
                );

        verify(repository, never())
                .findById(
                        payout.id().value()
                );
    }

    @Test
    void shouldInsertRequestedPayout() {
        Payout payout =
                requestedPayout();

        when(
                repository.findById(
                        payout.id().value()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                repository.save(any())
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        Payout result =
                adapter.save(payout);

        assertSame(
                payout,
                result
        );

        verify(repository)
                .save(
                        argThat(entity ->
                                entity.getId().equals(
                                        payout.id().value()
                                )
                                        && entity.getStatus()
                                        .equals("REQUESTED")
                                        && entity.getRequestedAt()
                                        .equals(
                                                LocalDateTime.of(
                                                        2026,
                                                        9,
                                                        24,
                                                        16,
                                                        0
                                                )
                                        )
                                        && entity.getCompletedAt()
                                        == null
                        )
                );
    }

    @Test
    void shouldUpdateProcessingPayoutWithoutChangingRequestedAt() {
        Payout payout =
                requestedPayout();

        LocalDateTime requestedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        10,
                        0
                );

        PayoutJpaEntity existing =
                mapper.toNewEntity(
                        payout,
                        requestedAt
                );

        existing.setVersion(5L);

        when(
                repository.findById(
                        payout.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        payout.markProcessing();

        adapter.save(payout);

        assertEquals(
                "PROCESSING",
                existing.getStatus()
        );

        assertEquals(
                requestedAt,
                existing.getRequestedAt()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        16,
                        0
                ),
                existing.getUpdatedAt()
        );

        assertNull(
                existing.getCompletedAt()
        );

        assertEquals(
                5L,
                existing.getVersion()
        );
    }

    @Test
    void shouldSetCompletedAtWhenPayoutSucceeds() {
        Payout payout =
                requestedPayout();

        payout.markProcessing();

        PayoutJpaEntity existing =
                mapper.toNewEntity(
                        payout,
                        LocalDateTime.of(
                                2026,
                                9,
                                24,
                                10,
                                0
                        )
                );

        existing.setVersion(8L);

        when(
                repository.findById(
                        payout.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        payout.markSucceeded(
                "BANK-REF-001"
        );

        adapter.save(payout);

        assertEquals(
                "SUCCESS",
                existing.getStatus()
        );

        assertEquals(
                "BANK-REF-001",
                existing.getProviderRef()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        16,
                        0
                ),
                existing.getCompletedAt()
        );

        assertEquals(
                8L,
                existing.getVersion()
        );
    }

    @Test
    void shouldSetCompletedAtWhenPayoutFails() {
        Payout payout =
                requestedPayout();

        payout.markProcessing();

        PayoutJpaEntity existing =
                mapper.toNewEntity(
                        payout,
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
                        payout.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        payout.markFailed(
                "BANK_TRANSFER_FAILED"
        );

        adapter.save(payout);

        assertEquals(
                "FAILED",
                existing.getStatus()
        );

        assertEquals(
                "BANK_TRANSFER_FAILED",
                existing.getFailureCode()
        );

        assertNotNull(
                existing.getCompletedAt()
        );
    }

    @Test
    void shouldNotReplaceExistingCompletedAt() {
        Payout payout =
                Payout.rehydrate(
                        new PayoutId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        Money.vnd(200_000),
                        new BankAccountSnapshot(
                                "VCB",
                                "NGUYEN VAN A",
                                "******1234"
                        ),
                        new IdempotencyKey(
                                "payout-idem-001"
                        ),
                        PayoutStatus.SUCCESS,
                        "BANK-REF-001",
                        null
                );

        PayoutJpaEntity existing =
                mapper.toNewEntity(
                        payout,
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
                        payout.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        adapter.save(payout);

        assertEquals(
                completedAt,
                existing.getCompletedAt()
        );
    }

    @Test
    void shouldReturnEmptyWhenPayoutDoesNotExist() {
        PayoutId payoutId =
                new PayoutId(
                        UUID.randomUUID()
                );

        when(
                repository.findById(
                        payoutId.value()
                )
        ).thenReturn(
                Optional.empty()
        );

        assertTrue(
                adapter.findById(
                        payoutId
                ).isEmpty()
        );
    }

    private Payout requestedPayout() {
        Payout payout =
                Payout.request(
                        new PayoutId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        Money.vnd(200_000),
                        new BankAccountSnapshot(
                                "VCB",
                                "NGUYEN VAN A",
                                "******1234"
                        ),
                        new IdempotencyKey(
                                "payout-idem-001"
                        )
                );

        payout.clearDomainEvents();

        return payout;
    }
}