package com.taca.paymentwallet.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetPaymentResult(
        UUID paymentId,
        UUID checkoutGroupId,
        UUID buyerUserId,
        String status,
        String method,
        long amount,
        String currency,
        long capturedAmount,
        long refundedAmount,
        Instant expiresAt,
        Instant paidAt,
        List<GetPaymentOrderResult> orders
) {

    public GetPaymentResult {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (checkoutGroupId == null) {
            throw new IllegalArgumentException("checkoutGroupId must not be null");
        }

        if (buyerUserId == null) {
            throw new IllegalArgumentException("buyerUserId must not be null");
        }

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }

        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (!"VND".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("currency must be VND");
        }

        if (capturedAmount < 0) {
            throw new IllegalArgumentException("capturedAmount must not be negative");
        }

        if (refundedAmount < 0) {
            throw new IllegalArgumentException("refundedAmount must not be negative");
        }

        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("orders must not be empty");
        }

        status = status.trim().toUpperCase();
        method = method.trim().toUpperCase();
        currency = "VND";
        orders = List.copyOf(orders);
    }
}