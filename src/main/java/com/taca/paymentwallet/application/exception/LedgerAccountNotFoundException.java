package com.taca.paymentwallet.application.exception;

public class LedgerAccountNotFoundException extends ApplicationException {

    public LedgerAccountNotFoundException(String description) {
        super("ledger account not found: " + description);
    }
}