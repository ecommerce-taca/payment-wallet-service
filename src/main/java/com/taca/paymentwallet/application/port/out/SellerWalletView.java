package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record SellerWalletView(
        UUID walletId,
        UUID shopId,
        long availableBalance,
        long pendingBalance,
        String currency,
        String status,
        Instant asOf
) {

    public SellerWalletView {
        if (walletId == null) {
            throw new IllegalArgumentException("walletId must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (availableBalance < 0 || pendingBalance < 0) {
            throw new IllegalArgumentException("wallet balances must not be negative");
        }

        if (!"VND".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("currency must be VND");
        }

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }

        if (asOf == null) {
            throw new IllegalArgumentException("asOf must not be null");
        }

        currency = "VND";
        status = status.trim().toUpperCase();
    }
}