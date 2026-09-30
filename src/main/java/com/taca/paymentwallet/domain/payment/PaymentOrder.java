package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

public record PaymentOrder(
        OrderId orderId,
        ShopId shopId,
        Money merchandiseAmount,
        Money shippingFee
) {

    public PaymentOrder {
        if (orderId == null) {
            throw new IllegalArgumentException(
                    "orderId must not be null"
            );
        }

        if (shopId == null) {
            throw new IllegalArgumentException(
                    "shopId must not be null"
            );
        }

        if (merchandiseAmount == null
                || !merchandiseAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "merchandiseAmount must be positive"
            );
        }

        if (shippingFee == null) {
            throw new IllegalArgumentException(
                    "shippingFee must not be null"
            );
        }

        if (!merchandiseAmount.currency()
                .equals(shippingFee.currency())) {
            throw new IllegalArgumentException(
                    "merchandiseAmount and shippingFee must use the same currency"
            );
        }
    }

    public PaymentOrder(
            OrderId orderId,
            ShopId shopId,
            Money merchandiseAmount
    ) {
        this(
                orderId,
                shopId,
                merchandiseAmount,
                new Money(
                        0,
                        merchandiseAmount.currency()
                )
        );
    }

    public Money totalAmount() {
        return merchandiseAmount.add(
                shippingFee
        );
    }

    public Money amount() {
        return totalAmount();
    }
}