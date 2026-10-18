package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.exception.WalletNotFoundException;
import com.taca.paymentwallet.application.port.out.SellerWalletReadPort;
import com.taca.paymentwallet.application.port.out.SellerWalletView;
import com.taca.paymentwallet.application.query.GetSellerWalletQuery;
import com.taca.paymentwallet.application.result.GetSellerWalletResult;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetSellerWalletServiceTest {

    private static final Instant AS_OF =
            Instant.parse("2026-08-31T04:00:00Z");

    @Test
    void shouldReturnSellerWallet() {
        UUID walletId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        SellerWalletReadPort repository =
                mock(SellerWalletReadPort.class);

        when(repository.findByShopIdAndCurrency(
                new ShopId(shopId),
                "VND"
        )).thenReturn(
                Optional.of(
                        new SellerWalletView(
                                walletId,
                                shopId,
                                12_500_000,
                                3_200_000,
                                "VND",
                                "ACTIVE",
                                AS_OF
                        )
                )
        );

        GetSellerWalletService service =
                new GetSellerWalletService(repository);

        GetSellerWalletResult result =
                service.execute(new GetSellerWalletQuery(shopId));

        assertEquals(walletId, result.walletId());
        assertEquals(shopId, result.shopId());
        assertEquals(12_500_000L, result.availableBalance());
        assertEquals(3_200_000L, result.pendingBalance());
        assertEquals("VND", result.currency());
        assertEquals("ACTIVE", result.status());
        assertEquals(AS_OF, result.asOf());

        verify(repository).findByShopIdAndCurrency(
                new ShopId(shopId),
                "VND"
        );
    }

    @Test
    void shouldReturnFrozenWallet() {
        UUID shopId = UUID.randomUUID();

        SellerWalletReadPort repository =
                mock(SellerWalletReadPort.class);

        when(repository.findByShopIdAndCurrency(
                new ShopId(shopId),
                "VND"
        )).thenReturn(
                Optional.of(
                        new SellerWalletView(
                                UUID.randomUUID(),
                                shopId,
                                500_000,
                                100_000,
                                "VND",
                                "FROZEN",
                                AS_OF
                        )
                )
        );

        GetSellerWalletService service =
                new GetSellerWalletService(repository);

        GetSellerWalletResult result =
                service.execute(new GetSellerWalletQuery(shopId));

        assertEquals("FROZEN", result.status());
    }

    @Test
    void shouldThrowWhenWalletDoesNotExist() {
        UUID shopId = UUID.randomUUID();

        SellerWalletReadPort repository =
                mock(SellerWalletReadPort.class);

        when(repository.findByShopIdAndCurrency(
                new ShopId(shopId),
                "VND"
        )).thenReturn(Optional.empty());

        GetSellerWalletService service =
                new GetSellerWalletService(repository);

        assertThrows(
                WalletNotFoundException.class,
                () -> service.execute(new GetSellerWalletQuery(shopId))
        );
    }

    @Test
    void shouldRejectNullQuery() {
        SellerWalletReadPort repository =
                mock(SellerWalletReadPort.class);

        GetSellerWalletService service =
                new GetSellerWalletService(repository);

        assertThrows(
                NullPointerException.class,
                () -> service.execute(null)
        );

        verifyNoInteractions(repository);
    }
}