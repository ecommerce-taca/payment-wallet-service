package com.taca.paymentwallet.domain.valueobject;

public record RateBps(int value) {

    public RateBps {
        if (value < 0 || value > 10_000) {
            throw new IllegalArgumentException("rateBps must be between 0 and 10000");
        }
    }

    public static RateBps of(int value) {
        return new RateBps(value);
    }
}
