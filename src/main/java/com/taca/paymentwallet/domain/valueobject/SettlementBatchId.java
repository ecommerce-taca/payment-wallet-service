package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record SettlementBatchId(UUID value) {
    
    public SettlementBatchId {
        if (value == null) {
            throw new IllegalArgumentException("settlementBatchId must not be null");
        }
    }
}
