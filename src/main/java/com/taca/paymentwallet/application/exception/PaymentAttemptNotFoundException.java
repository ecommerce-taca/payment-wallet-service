package com.taca.paymentwallet.application.exception;

public class PaymentAttemptNotFoundException extends ApplicationException {

    public PaymentAttemptNotFoundException(
            String provider,
            String providerTransactionRef
    ) {
        super(
                "Payment attempt not found: "
                        + provider
                        + "/"
                        + providerTransactionRef
        );
    }
}