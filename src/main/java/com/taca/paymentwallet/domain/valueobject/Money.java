package com.taca.paymentwallet.domain.valueobject;

import java.util.Objects;

public record Money(long amount, String currency) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        currency = currency.toUpperCase();

        if (!Objects.equals(currency, "VND")) {
            throw new IllegalArgumentException("only VND is supported in v1");
        }
    }

    public static Money vnd(long amount) {
        return new Money(amount, "VND");
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(amount, other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);

        if (amount < other.amount) {
            throw new IllegalArgumentException("result amount must not be negative");
        }

        return new Money(amount - other.amount, currency);
    }

    public Money multiplyBy(RateBps rate) {
        long result = Math.round((double) amount * rate.value() / 10_000);
        return new Money(result, currency);
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount > other.amount;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        requireSameCurrency(other);
        return amount >= other.amount;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch");
        }
    }
}
