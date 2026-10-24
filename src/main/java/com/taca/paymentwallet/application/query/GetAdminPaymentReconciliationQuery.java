package com.taca.paymentwallet.application.query;

public record GetAdminPaymentReconciliationQuery(
        int page,
        int size
) {

    public GetAdminPaymentReconciliationQuery {
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}