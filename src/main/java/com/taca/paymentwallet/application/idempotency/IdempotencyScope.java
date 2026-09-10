package com.taca.paymentwallet.application.idempotency;

import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.util.Objects;

public record IdempotencyScope(String value) {

    public IdempotencyScope {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("idempotency scope must not be blank");
        }

        value = value.trim();
    }

    public static IdempotencyScope payment(CheckoutGroupId checkoutGroupId) {
        Objects.requireNonNull(checkoutGroupId, "checkoutGroupId must not be null");

        return new IdempotencyScope("PAYMENT:" + checkoutGroupId.value());
    }

    public static IdempotencyScope refund(PaymentId paymentId) {
        Objects.requireNonNull(paymentId, "paymentId must not be null");

        return new IdempotencyScope("REFUND:" + paymentId.value());
    }

    public static IdempotencyScope payout(ShopId shopId) {
        Objects.requireNonNull(shopId, "shopId must not be null");

        return new IdempotencyScope("PAYOUT:" + shopId.value());
    }
}