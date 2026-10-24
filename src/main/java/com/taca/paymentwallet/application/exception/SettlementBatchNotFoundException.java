package com.taca.paymentwallet.application.exception;

import java.util.UUID;

public class SettlementBatchNotFoundException extends ApplicationException {

    public SettlementBatchNotFoundException(UUID batchId) {
        super("Settlement batch not found: " + batchId);
    }
}