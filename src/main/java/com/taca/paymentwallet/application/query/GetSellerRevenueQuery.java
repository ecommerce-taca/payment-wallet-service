package com.taca.paymentwallet.application.query;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record GetSellerRevenueQuery(
        UUID shopId,
        LocalDate from,
        LocalDate to,
        RevenueGranularity granularity
) {

    private static final long MAX_RANGE_DAYS = 366;

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

        long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;

        if (rangeDays > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("revenue range must not exceed 366 days");
        }
    }
}