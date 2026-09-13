package com.taca.paymentwallet.domain.payout;

import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayoutTest {

    @Test
    void shouldCreateRequestedPayout() {
        Payout payout = createPayout();

        assertEquals(PayoutStatus.REQUESTED, payout.status());
    }

    @Test
    void shouldMarkPayoutProcessing() {
        Payout payout = createPayout();

        payout.markProcessing();

        assertEquals(PayoutStatus.PROCESSING, payout.status());
    }

    @Test
    void shouldMarkPayoutSucceededFromProcessing() {
        Payout payout = createPayout();

        payout.markProcessing();
        payout.markSucceeded("BANK_TXN_001");

        assertEquals(PayoutStatus.SUCCESS, payout.status());
        assertEquals("BANK_TXN_001", payout.providerRef());
    }

    @Test
    void shouldMarkPayoutFailedFromProcessing() {
        Payout payout = createPayout();

        payout.markProcessing();
        payout.markFailed("BANK_REJECTED");

        assertEquals(PayoutStatus.FAILED, payout.status());
        assertEquals("BANK_REJECTED", payout.failureCode());
    }

    @Test
    void shouldCancelRequestedPayout() {
        Payout payout = createPayout();

        payout.cancel();

        assertEquals(PayoutStatus.CANCELLED, payout.status());
    }

    @Test
    void shouldRejectSuccessWithoutProcessing() {
        Payout payout = createPayout();

        assertThrows(InvalidPayoutStateException.class,
                () -> payout.markSucceeded("BANK_TXN_001"));
    }

    @Test
    void shouldRejectRawBankAccountNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> new BankAccountSnapshot(
                        "VCB",
                        "NGUYEN VAN A",
                        "1234567890"
                ));
    }

    private Payout createPayout() {
        return Payout.request(
                new PayoutId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                Money.vnd(100_000),
                new BankAccountSnapshot(
                        "VCB",
                        "NGUYEN VAN A",
                        "******7890"
                ),
                new IdempotencyKey(UUID.randomUUID().toString())
        );
    }
}
