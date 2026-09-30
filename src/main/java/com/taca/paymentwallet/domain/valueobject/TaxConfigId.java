package com.taca.paymentwallet.domain.valueobject;

import java.util.UUID;

public record TaxConfigId(UUID value) {

    public TaxConfigId {
        if (value == null) {
            throw new IllegalArgumentException("taxConfigId must not be null");
        }
    }
}