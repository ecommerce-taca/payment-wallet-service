package com.taca.paymentwallet.application.port.out;

import java.time.LocalDate;

public record SellerRevenueDailyView(
        LocalDate date,
        long gross,
        long commission,
        long tax,
        long net,
        long refunded,
        long orderCount
) {

    public SellerRevenueDailyView {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }

        if (gross < 0 || commission < 0 || tax < 0
                || net < 0 || refunded < 0 || orderCount < 0) {
            throw new IllegalArgumentException("revenue values must not be negative");
        }
    }
}