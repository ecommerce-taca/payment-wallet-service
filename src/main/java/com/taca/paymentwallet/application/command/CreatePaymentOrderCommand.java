package com.taca.paymentwallet.application.command;

import java.util.UUID;

public record CreatePaymentOrderCommand(
        UUID orderId,
        UUID shopId,
        long amount,
        String currency
) {
}
