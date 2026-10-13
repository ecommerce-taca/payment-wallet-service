package com.taca.paymentwallet.application.query;

import java.time.LocalDate;
import java.util.UUID;

public record GetSellerRevenueQuery(
        UUID shopId,
        LocalDate from,
        LocalDate to,
        RevenueGranularity granularity
) {

    public GetSellerRevenueQuery {
        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (from == null) {
            throw new IllegalArgumentException("from must not be null");
        }

        if (to == null) {
            throw new IllegalArgumentException("to must not be null");
        }

        if (granularity == null) {
            throw new IllegalArgumentException("granularity must not be null");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }
}