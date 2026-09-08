package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        if (value == null) {
            throw new IllegalArgumentException("ShopId must not be null");
        }
    }
}