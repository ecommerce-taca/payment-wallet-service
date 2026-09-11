package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;

public class PayoutAmountMismatchException extends ApplicationException {

    public PayoutAmountMismatchException(
            PayoutId payoutId,
            Money expected,
            Money actual
    ) {
        super("Payout amount mismatch for payout "
                + payoutId.value()
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