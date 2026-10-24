package com.taca.paymentwallet.application.query;

import java.util.UUID;

public record GetAdminSettlementDetailQuery(UUID batchId) {

    public GetAdminSettlementDetailQuery {
        if (batchId == null) {
            throw new IllegalArgumentException("batchId must not be null");
        }
    }
}