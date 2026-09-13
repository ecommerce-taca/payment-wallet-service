package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletTest {

    @Test
    void shouldCreateActiveWalletWithZeroBalance() {
        Wallet wallet = createWallet();

        assertEquals(WalletStatus.ACTIVE, wallet.status());
        assertEquals(Money.vnd(0), wallet.availableBalance());
        assertEquals(Money.vnd(0), wallet.pendingBalance());
    }

    @Test
    void shouldCreditPendingBalance() {
        Wallet wallet = createWallet();

        wallet.creditPending(Money.vnd(100_000));

        assertEquals(Money.vnd(100_000), wallet.pendingBalance());
        assertEquals(Money.vnd(0), wallet.availableBalance());
    }

    @Test
    void shouldReleasePendingToAvailable() {
        Wallet wallet = createWallet();
        wallet.creditPending(Money.vnd(100_000));

        wallet.releasePendingToAvailable(Money.vnd(70_000));

        assertEquals(Money.vnd(30_000), wallet.pendingBalance());
        assertEquals(Money.vnd(70_000), wallet.availableBalance());
    }

    @Test
    void shouldRejectReleaseWhenPendingBalanceIsInsufficient() {
        Wallet wallet = createWallet();
        wallet.creditPending(Money.vnd(50_000));

        assertThrows(InsufficientWalletBalanceException.class,
                () -> wallet.releasePendingToAvailable(Money.vnd(70_000)));
    }

    @Test
    void shouldReservePayoutFromAvailableBalance() {
        Wallet wallet = createWallet();
        wallet.creditPending(Money.vnd(100_000));
        wallet.releasePendingToAvailable(Money.vnd(100_000));

        wallet.reservePayout(Money.vnd(40_000));

        assertEquals(Money.vnd(60_000), wallet.availableBalance());
    }

    @Test
    void shouldRejectPayoutWhenAvailableBalanceIsInsufficient() {
        Wallet wallet = createWallet();

        assertThrows(InsufficientWalletBalanceException.class,
                () -> wallet.reservePayout(Money.vnd(10_000)));
    }

    @Test
    void shouldNotOperateFrozenWallet() {
        Wallet wallet = createWallet();
        wallet.freeze();

        assertThrows(IllegalStateException.class,
                () -> wallet.creditPending(Money.vnd(10_000)));
    }

    private Wallet createWallet() {
        return Wallet.create(
                new WalletId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID())
        );
    }
}