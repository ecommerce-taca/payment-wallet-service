package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record PayoutId(UUID value) {

    public PayoutId {
        if (value == null) {
            throw new IllegalArgumentException("payoutId must not be null");
        }
    }
}