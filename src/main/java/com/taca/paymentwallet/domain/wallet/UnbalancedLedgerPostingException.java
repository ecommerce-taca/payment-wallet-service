package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.exception.DomainException;

public class UnbalancedLedgerPostingException extends DomainException {

    public UnbalancedLedgerPostingException(long totalDebit, long totalCredit) {
        super("Ledger posting is unbalanced: totalDebit="
                + totalDebit
                + ", totalCredit="
                + totalCredit);
    }
}
