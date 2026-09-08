package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

public record PaymentOrder(
        OrderId orderId,
        ShopId shopId,
        Money amount
) {

    public PaymentOrder {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        if (shopId == null) throw new IllegalArgumentException("shopId must not be null");
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}