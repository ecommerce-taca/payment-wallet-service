package com.taca.paymentwallet.application.exception;

public class UnsupportedPaymentMethodException extends ApplicationException {

    public UnsupportedPaymentMethodException(String method) {
        super("Unsupported payment method: " + method);
    }
}
