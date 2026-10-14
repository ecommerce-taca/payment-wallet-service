package com.taca.paymentwallet.presentation.rest.seller;

import java.util.List;
import java.util.Objects;

public record SellerRevenueData(
        SellerRevenueRangeData range,
        String currency,
        SellerRevenueSummaryData summary,
        List<SellerRevenueBucketData> buckets
) {

    public SellerRevenueData {
        Objects.requireNonNull(range, "range must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(buckets, "buckets must not be null");

        if (!"VND".equals(currency)) {
            throw new IllegalArgumentException("currency must be VND");
        }

        buckets = List.copyOf(buckets);
    }
}