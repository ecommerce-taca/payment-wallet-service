package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record ShopId(UUID value) {

    public ShopId {
        if (value == null) {
            throw new IllegalArgumentException("ShopId must not be null");
        }
    }
}
