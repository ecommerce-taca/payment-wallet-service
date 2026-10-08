package com.taca.paymentwallet.application.command;

import java.util.UUID;

public record CreatePaymentOrderCommand(
        UUID orderId,
        UUID shopId,
        long amount,
        long shippingFee
) {

    public CreatePaymentOrderCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("order amount must be positive");
        }

        if (shippingFee < 0) {
            throw new IllegalArgumentException("shippingFee must not be negative");
        }

        if (shippingFee >= amount) {
            throw new IllegalArgumentException("shippingFee must be less than order amount");
        }
    }

    public long merchandiseAmount() {
        return amount - shippingFee;
    }
}