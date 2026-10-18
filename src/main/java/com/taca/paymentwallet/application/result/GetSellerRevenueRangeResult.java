package com.taca.paymentwallet.application.result;

import java.time.LocalDate;

public record GetSellerRevenueRangeResult(
        LocalDate from,
        LocalDate to,
        String granularity
) {
}