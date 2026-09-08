package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record BuyerUserId(UUID value) {

    public BuyerUserId {
        if (value == null) {
            throw new IllegalArgumentException("buyerUserId must not be null");
        }
    }
}