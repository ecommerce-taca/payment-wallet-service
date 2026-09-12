package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.PaymentId;

public class PaymentNotFoundException extends ApplicationException {

    public PaymentNotFoundException(PaymentId paymentId) {
        super("Payment not found: " + paymentId.value());
    }
}
