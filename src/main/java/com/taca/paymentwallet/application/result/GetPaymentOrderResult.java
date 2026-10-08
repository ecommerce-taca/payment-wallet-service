package com.taca.paymentwallet.application.result;

import java.util.UUID;

public record GetPaymentOrderResult(
        UUID orderId,
        UUID shopId,
        long merchandiseAmount,
        long shippingFee,
        long amount
) {

    public GetPaymentOrderResult {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (merchandiseAmount <= 0) {
            throw new IllegalArgumentException("merchandiseAmount must be positive");
        }

        if (shippingFee < 0) {
            throw new IllegalArgumentException("shippingFee must not be negative");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (merchandiseAmount + shippingFee != amount) {
            throw new IllegalArgumentException(
                    "order amount must equal merchandiseAmount plus shippingFee"
            );
        }
    }
}