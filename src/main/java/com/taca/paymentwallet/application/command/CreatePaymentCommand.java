package com.taca.paymentwallet.application.command;

import java.util.List;
import java.util.UUID;

public record CreatePaymentCommand(
        UUID checkoutGroupId,
        UUID buyerUserId,
        String method,
        long amount,
        String currency,
        String idempotencyKey,
        List<CreatePaymentOrderCommand> orders
) {
}