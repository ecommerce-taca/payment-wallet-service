package com.taca.paymentwallet.domain.valueobject;

public record Money(long amount, String currency) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
    }

    public boolean isPositive() {
        return amount > 0;
    }
}
