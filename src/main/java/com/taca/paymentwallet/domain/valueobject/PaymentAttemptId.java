package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record PaymentAttemptId(UUID value) {

    public PaymentAttemptId {
        if (value == null) {
            throw new IllegalArgumentException(
                    "value must not be null"
            );
        }
    }
}