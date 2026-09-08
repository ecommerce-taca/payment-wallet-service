package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.Money;

public record LedgerEntry(
        LedgerAccountId accountId,
        LedgerEntryType entryType,
        Money amount
) {

    public LedgerEntry {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId must not be null");
        }
        if (entryType == null) {
            throw new IllegalArgumentException("entryType must not be null");
        }
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    public static LedgerEntry debit(LedgerAccountId accountId, Money amount) {
        return new LedgerEntry(
                accountId,
                LedgerEntryType.DEBIT,
                amount
        );
    }

    public static LedgerEntry credit(LedgerAccountId accountId, Money amount) {
        return new LedgerEntry(
                accountId,
                LedgerEntryType.CREDIT,
                amount
        );
    }
}
