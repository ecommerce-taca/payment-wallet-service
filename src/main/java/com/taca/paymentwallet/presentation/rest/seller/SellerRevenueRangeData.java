package com.taca.paymentwallet.presentation.rest.seller;

import java.time.LocalDate;

public record SellerRevenueRangeData(
        LocalDate from,
        LocalDate to,
        String granularity
) {
}