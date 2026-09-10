package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record RefundId(UUID value) {

    public RefundId {
        if (value == null) {
            throw new IllegalArgumentException("refundId must not be null");
        }
    }
}
