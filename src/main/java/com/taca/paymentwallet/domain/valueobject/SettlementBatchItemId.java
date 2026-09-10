package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record SettlementBatchItemId(UUID value) {

    public SettlementBatchItemId {
        if (value == null) {
            throw new IllegalArgumentException("settlementBatchItemId must not be null");
        }
    }
}
