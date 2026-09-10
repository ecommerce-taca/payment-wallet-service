package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record PaymentAllocationId(UUID value) {

    public PaymentAllocationId {
        if (value == null) {
            throw new IllegalArgumentException("paymentAllocationId must not be null");
        }
    }
}