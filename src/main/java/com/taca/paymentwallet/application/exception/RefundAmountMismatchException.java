package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.RefundId;

public class RefundAmountMismatchException extends ApplicationException {

    public RefundAmountMismatchException(
            RefundId refundId,
            Money expected,
            Money actual
    ) {
        super("Refund amount mismatch for refund "
                + refundId.value()
                + ", expected="
                + expected.amount()
                + " "
                + expected.currency()
                + ", actual="
                + actual.amount()
                + " "
                + actual.currency());
    }
}
