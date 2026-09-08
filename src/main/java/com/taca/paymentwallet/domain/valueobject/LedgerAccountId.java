package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record LedgerAccountId(UUID value) {

    public LedgerAccountId {
        if (value == null) {
            throw new IllegalArgumentException("ledgerAccountId must not be null");
        }
    }
}
