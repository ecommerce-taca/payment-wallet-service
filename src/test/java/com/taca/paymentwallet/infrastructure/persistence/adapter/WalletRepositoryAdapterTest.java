package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.domain.wallet.Wallet;
import com.taca.paymentwallet.infrastructure.persistence.entity.WalletJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.WalletPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.WalletJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletRepositoryAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-24T12:00:00Z"
            );

    private WalletJpaRepository walletRepository;
    private ClockPort clockPort;

    private WalletRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        walletRepository =
                mock(WalletJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(NOW);

        adapter =
                new WalletRepositoryAdapter(
                        walletRepository,
                        new WalletPersistenceMapper(),
                        clockPort
                );
    }

    @Test
    void shouldFindWalletById() {
        Wallet wallet =
                wallet();

        WalletJpaEntity entity =
                entityFrom(wallet);

        when(
                walletRepository.findById(
                        wallet.id().value()
                )
        ).thenReturn(
                Optional.of(entity)
        );

        Optional<Wallet> result =
                adapter.findById(
                        wallet.id()
                );

        assertTrue(
                result.isPresent()
        );

        assertEquals(
                wallet.id(),
                result.get().id()
        );

        assertEquals(
                wallet.shopId(),
                result.get().shopId()
        );

        assertEquals(
                wallet.availableBalance(),
                result.get().availableBalance()
        );

        assertEquals(
                wallet.pendingBalance(),
                result.get().pendingBalance()
        );

        assertEquals(
                wallet.status(),
                result.get().status()
        );
    }

    @Test
    void shouldUsePessimisticLookupForFindByIdForUpdate() {
        Wallet wallet =
                wallet();

        WalletJpaEntity entity =
                entityFrom(wallet);

        when(
                walletRepository.findByIdForUpdate(
                        wallet.id().value()
                )
        ).thenReturn(
                Optional.of(entity)
        );

        Optional<Wallet> result =
                adapter.findByIdForUpdate(
                        wallet.id()
                );

        assertTrue(
                result.isPresent()
        );

        verify(walletRepository)
                .findByIdForUpdate(
                        wallet.id().value()
                );

        verify(walletRepository, never())
                .findById(
                        wallet.id().value()
                );
    }

    @Test
    void shouldFindWalletByShopAndCurrencyForUpdate() {
        Wallet wallet =
                wallet();

        WalletJpaEntity entity =
                entityFrom(wallet);

        when(
                walletRepository
                        .findByShopIdAndCurrencyForUpdate(
                                wallet.shopId().value(),
                                "VND"
                        )
        ).thenReturn(
                Optional.of(entity)
        );

        Optional<Wallet> result =
                adapter.findByShopIdAndCurrencyForUpdate(
                        wallet.shopId(),
                        "vnd"
                );

        assertTrue(
                result.isPresent()
        );

        assertEquals(
                wallet.id(),
                result.get().id()
        );

        verify(walletRepository)
                .findByShopIdAndCurrencyForUpdate(
                        wallet.shopId().value(),
                        "VND"
                );
    }

    @Test
    void shouldInsertWalletWhenWalletIsNew() {
        Wallet wallet =
                wallet();

        when(
                walletRepository.findById(
                        wallet.id().value()
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                walletRepository.save(any())
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        Wallet result =
                adapter.save(wallet);

        assertSame(
                wallet,
                result
        );

        ArgumentCaptor<WalletJpaEntity> captor =
                ArgumentCaptor.forClass(
                        WalletJpaEntity.class
                );

        verify(walletRepository)
                .save(
                        captor.capture()
                );

        WalletJpaEntity saved =
                captor.getValue();

        assertEquals(
                wallet.id().value(),
                saved.getId()
        );

        assertEquals(
                wallet.shopId().value(),
                saved.getShopId()
        );

        assertEquals(
                "VND",
                saved.getCurrency()
        );

        assertEquals(
                0L,
                saved.getAvailableBalance()
        );

        assertEquals(
                0L,
                saved.getPendingBalance()
        );

        assertEquals(
                "ACTIVE",
                saved.getStatus()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        12,
                        0
                ),
                saved.getCreatedAt()
        );

        assertEquals(
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    @Test
    void shouldUpdateExistingWalletWithoutReplacingPersistenceMetadata() {
        Wallet wallet =
                wallet();

        WalletJpaEntity existing =
                entityFrom(wallet);

        LocalDateTime originalCreatedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        20,
                        10,
                        0
                );

        existing.setCreatedAt(
                originalCreatedAt
        );

        existing.setUpdatedAt(
                originalCreatedAt
        );

        existing.setVersion(8L);

        when(
                walletRepository.findById(
                        wallet.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        wallet.creditPending(
                Money.vnd(92_000)
        );

        Wallet result =
                adapter.save(wallet);

        assertSame(
                wallet,
                result
        );

        assertEquals(
                92_000L,
                existing.getPendingBalance()
        );

        assertEquals(
                0L,
                existing.getAvailableBalance()
        );

        assertEquals(
                8L,
                existing.getVersion()
        );

        assertEquals(
                originalCreatedAt,
                existing.getCreatedAt()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        12,
                        0
                ),
                existing.getUpdatedAt()
        );

        verify(walletRepository)
                .save(existing);
    }

    @Test
    void shouldPersistReleasedWalletBalance() {
        Wallet wallet =
                wallet();

        wallet.creditPending(
                Money.vnd(92_000)
        );

        wallet.releasePendingToAvailable(
                Money.vnd(92_000)
        );

        WalletJpaEntity existing =
                entityFrom(
                        Wallet.rehydrate(
                                wallet.id(),
                                wallet.shopId(),
                                "VND",
                                Money.vnd(0),
                                Money.vnd(92_000),
                                wallet.status()
                        )
                );

        existing.setVersion(3L);

        when(
                walletRepository.findById(
                        wallet.id().value()
                )
        ).thenReturn(
                Optional.of(existing)
        );

        adapter.save(wallet);

        assertEquals(
                92_000L,
                existing.getAvailableBalance()
        );

        assertEquals(
                0L,
                existing.getPendingBalance()
        );

        assertEquals(
                3L,
                existing.getVersion()
        );
    }

    @Test
    void shouldReturnEmptyWhenWalletDoesNotExist() {
        WalletId walletId =
                new WalletId(
                        UUID.randomUUID()
                );

        when(
                walletRepository.findById(
                        walletId.value()
                )
        ).thenReturn(
                Optional.empty()
        );

        Optional<Wallet> result =
                adapter.findById(
                        walletId
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldRejectBlankCurrency() {
        ShopId shopId =
                new ShopId(
                        UUID.randomUUID()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> adapter
                        .findByShopIdAndCurrencyForUpdate(
                                shopId,
                                "   "
                        )
        );

        verifyNoInteractions(
                walletRepository
        );
    }

    private Wallet wallet() {
        return Wallet.create(
                new WalletId(
                        UUID.randomUUID()
                ),
                new ShopId(
                        UUID.randomUUID()
                )
        );
    }

    private WalletJpaEntity entityFrom(
            Wallet wallet
    ) {
        return new WalletPersistenceMapper()
                .toNewEntity(
                        wallet,
                        LocalDateTime.of(
                                2026,
                                9,
                                24,
                                10,
                                0
                        )
                );
    }
}