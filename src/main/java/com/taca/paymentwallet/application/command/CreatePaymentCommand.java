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
        List<CreatePaymentOrderCommand> orders,
        String clientIp
) {

    public CreatePaymentCommand {
        if (checkoutGroupId == null) {
            throw new IllegalArgumentException("checkoutGroupId must not be null");
        }

        if (buyerUserId == null) {
            throw new IllegalArgumentException("buyerUserId must not be null");
        }

        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("orders must not be empty");
        }

        long totalOrderAmount = orders.stream()
                .mapToLong(CreatePaymentOrderCommand::amount)
                .sum();

        if (totalOrderAmount != amount) {
            throw new IllegalArgumentException("total order amount must equal payment amount");
        }

        method = method.trim().toUpperCase();
        currency = currency.trim().toUpperCase();
        idempotencyKey = idempotencyKey.trim();
        orders = List.copyOf(orders);
        clientIp = clientIp == null ? null : clientIp.trim();
    }
}