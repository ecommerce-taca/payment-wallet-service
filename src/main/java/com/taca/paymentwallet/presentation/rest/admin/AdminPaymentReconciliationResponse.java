package com.taca.paymentwallet.presentation.rest.admin;

import java.util.List;
import java.util.Objects;

public record AdminPaymentReconciliationResponse(
        List<AdminPaymentReconciliationData> data,
        AdminPaymentReconciliationMeta meta
) {

    public AdminPaymentReconciliationResponse {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(meta, "meta must not be null");
        data = List.copyOf(data);
    }
}