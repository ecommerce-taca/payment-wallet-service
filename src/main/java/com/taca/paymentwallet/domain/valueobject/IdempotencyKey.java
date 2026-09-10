package com.taca.paymentwallet.domain.valueobject;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        if (value.length() > 160) {
            throw new IllegalArgumentException("idempotencyKey is too long");
        }
    }
}
