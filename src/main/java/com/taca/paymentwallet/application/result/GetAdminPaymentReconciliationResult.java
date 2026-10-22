package com.taca.paymentwallet.application.result;

import java.util.List;

public record GetAdminPaymentReconciliationResult(
        List<GetAdminPaymentReconciliationItemResult> items,
        int page,
        int size,
        long total,
        int totalPages,
        long mismatchCount
) {

    public GetAdminPaymentReconciliationResult {
        if (items == null) {
            throw new IllegalArgumentException("items must not be null");
        }

        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }

        if (total < 0 || totalPages < 0 || mismatchCount < 0) {
            throw new IllegalArgumentException("pagination totals must not be negative");
        }

        items = List.copyOf(items);
    }
}