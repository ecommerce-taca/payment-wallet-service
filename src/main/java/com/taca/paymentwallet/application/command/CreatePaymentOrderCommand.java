package com.taca.paymentwallet.application.command;

import java.util.UUID;

public record CreatePaymentOrderCommand(
        UUID orderId,
        UUID shopId,
        long amount
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
    }
}