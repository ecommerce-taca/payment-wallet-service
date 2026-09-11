package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

public class PaymentAmountMismatchException extends ApplicationException {

    public PaymentAmountMismatchException(
            PaymentId paymentId,
            Money expected,
            Money actual
    ) {
        super("Payment amount mismatch for payment "
                + paymentId.value()
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
