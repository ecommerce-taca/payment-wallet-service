package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record WalletId(UUID value) {

    public WalletId {
        if (value == null) {
            throw new IllegalArgumentException("walletId must not be null");
        }
    }
}