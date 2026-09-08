package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record PaymentId(UUID value) {

    public PaymentId {
        if (value == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }
    }
}
