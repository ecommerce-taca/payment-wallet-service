package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerPostingTest {

    @Test
    void shouldCreateBalancedLedgerPosting() {
        LedgerAccountId debitAccountId = accountId();
        LedgerAccountId creditAccountId = accountId();

        LedgerPosting posting = new LedgerPosting(
                postingId(),
                "TEST_POSTING",
                "TEST_POSTING:1",
                "TEST",
                UUID.randomUUID().toString(),
                List.of(
                        LedgerEntry.debit(debitAccountId, Money.vnd(100_000)),
                        LedgerEntry.credit(creditAccountId, Money.vnd(100_000))
                )
        );

        assertEquals(2, posting.entries().size());
    }

    @Test
    void shouldRejectUnbalancedLedgerPosting() {
        LedgerAccountId debitAccountId = accountId();
        LedgerAccountId creditAccountId = accountId();

        assertThrows(UnbalancedLedgerPostingException.class, () -> new LedgerPosting(
                postingId(),
                "TEST_POSTING",
                "TEST_POSTING:2",
                "TEST",
                UUID.randomUUID().toString(),
                List.of(
                        LedgerEntry.debit(debitAccountId, Money.vnd(100_000)),
                        LedgerEntry.credit(creditAccountId, Money.vnd(90_000))
                )
        ));
    }

    private LedgerPostingId postingId() {
        return new LedgerPostingId(UUID.randomUUID());
    }

    private LedgerAccountId accountId() {
        return new LedgerAccountId(UUID.randomUUID());
    }
}
