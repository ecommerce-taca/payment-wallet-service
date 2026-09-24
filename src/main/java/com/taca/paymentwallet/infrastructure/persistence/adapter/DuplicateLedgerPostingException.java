package com.taca.paymentwallet.infrastructure.persistence.adapter;

public class DuplicateLedgerPostingException extends RuntimeException {

    public DuplicateLedgerPostingException(String businessKey) {
        super(
                "ledger posting already exists for business key "
                        + businessKey
        );
    }
}