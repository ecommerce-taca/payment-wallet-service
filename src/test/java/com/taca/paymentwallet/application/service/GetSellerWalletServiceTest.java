package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.WalletNotFoundException;
import com.taca.paymentwallet.application.port.out.WalletRepositoryPort;
import com.taca.paymentwallet.application.query.GetSellerWalletQuery;
import com.taca.paymentwallet.application.result.GetSellerWalletResult;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.domain.wallet.Wallet;
import com.taca.paymentwallet.domain.wallet.WalletStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GetSellerWalletServiceTest {

    @Test
    void shouldReturnSellerWallet() {
        UUID walletId = UUID.randomUUID();

        UUID shopId = UUID.randomUUID();

        Wallet wallet =
                Wallet.rehydrate(
                        new WalletId(walletId),
                        new ShopId(shopId),
                        "VND",
                        Money.vnd(12_500_000),
                        Money.vnd(3_200_000),
                        WalletStatus.ACTIVE
                );

        WalletRepositoryPort repository = mock(WalletRepositoryPort.class);

        when(
                repository.findByShopIdAndCurrency(
                        new ShopId(shopId),
                        "VND"
                )
        ).thenReturn(Optional.of(wallet));

        GetSellerWalletService service = new GetSellerWalletService(repository);

        GetSellerWalletResult result =
                service.execute(
                        new GetSellerWalletQuery(shopId)
                );

        assertEquals(walletId, result.walletId());

        assertEquals(shopId, result.shopId());

        assertEquals(12_500_000L, result.availableBalance());

        assertEquals(3_200_000L, result.pendingBalance());

        assertEquals("VND", result.currency());

        assertEquals("ACTIVE", result.status());

        verify(repository)
                .findByShopIdAndCurrency(
                        new ShopId(shopId),
                        "VND"
                );
    }

    @Test
    void shouldReturnFrozenWallet() {
        UUID shopId = UUID.randomUUID();

        Wallet wallet = Wallet.rehydrate(
                new WalletId(UUID.randomUUID()),
                new ShopId(shopId),
                "VND",
                Money.vnd(500_000),
                Money.vnd(100_000),
                WalletStatus.FROZEN
        );

        WalletRepositoryPort repository = mock(WalletRepositoryPort.class);

        when(
                repository.findByShopIdAndCurrency(
                        new ShopId(shopId),
                        "VND"
                )
        ).thenReturn(
                Optional.of(wallet)
        );

        GetSellerWalletService service = new GetSellerWalletService(repository);

        GetSellerWalletResult result =
                service.execute(
                        new GetSellerWalletQuery(shopId)
                );

        assertEquals("FROZEN", result.status());
    }

    @Test
    void shouldThrowWhenWalletDoesNotExist() {
        UUID shopId = UUID.randomUUID();

        WalletRepositoryPort repository = mock(WalletRepositoryPort.class);

        when(
                repository.findByShopIdAndCurrency(
                        new ShopId(shopId),
                        "VND"
                )
        ).thenReturn(
                Optional.empty()
        );

        GetSellerWalletService service = new GetSellerWalletService(repository);

        assertThrows(
                WalletNotFoundException.class,
                () ->
                        service.execute(
                                new GetSellerWalletQuery(
                                        shopId
                                )
                        )
        );
    }

    @Test
    void shouldRejectNullQuery() {
        WalletRepositoryPort repository = mock(WalletRepositoryPort.class);

        GetSellerWalletService service = new GetSellerWalletService(repository);

        assertThrows(
                NullPointerException.class,
                () -> service.execute(null)
        );

        verifyNoInteractions(repository);
    }
}