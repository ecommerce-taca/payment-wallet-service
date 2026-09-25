package com.taca.paymentwallet.application.exception;

public class PaymentProviderEventNotFoundException extends ApplicationException {

    public PaymentProviderEventNotFoundException(
            String provider,
            String providerEventId
    ) {
        super(
                "payment provider event not found: "
                        + provider
                        + "/"
                        + providerEventId
        );
    }
}