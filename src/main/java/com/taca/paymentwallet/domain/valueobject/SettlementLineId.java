package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record SettlementLineId(UUID value) {

    public SettlementLineId {
        if (value == null) {
            throw new IllegalArgumentException("settlementLineId must not be null");
        }
    }
}
