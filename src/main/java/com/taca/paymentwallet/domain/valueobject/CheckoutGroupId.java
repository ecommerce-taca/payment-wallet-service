package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record CheckoutGroupId(UUID value) {

    public CheckoutGroupId {
        if (value == null) {
            throw new IllegalArgumentException("checkoutGroupId must not be null");
        }
    }
}
