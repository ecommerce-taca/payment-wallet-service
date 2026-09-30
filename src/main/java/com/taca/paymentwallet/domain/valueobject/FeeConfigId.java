package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record FeeConfigId(UUID value) {

    public FeeConfigId {
        if (value == null) {
            throw new IllegalArgumentException("feeConfigId must not be null");
        }
    }
}