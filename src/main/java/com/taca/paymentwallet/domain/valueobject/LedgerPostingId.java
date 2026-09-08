package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record LedgerPostingId(UUID value) {

    public LedgerPostingId {
        if (value == null) {
            throw new IllegalArgumentException("ledgerPostingId must not be null");
        }
    }
}