package com.taca.paymentwallet.application.result;

import java.util.List;

public record GetSellerRevenueResult(
        GetSellerRevenueRangeResult range,
        String currency,
        GetSellerRevenueSummaryResult summary,
        List<GetSellerRevenueBucketResult> buckets
) {

    public GetSellerRevenueResult {
        if (range == null) {
            throw new IllegalArgumentException("range must not be null");
        }

        if (!"VND".equals(currency)) {
            throw new IllegalArgumentException("currency must be VND");
        }

        if (summary == null) {
            throw new IllegalArgumentException("summary must not be null");
        }

        if (buckets == null) {
            throw new IllegalArgumentException("buckets must not be null");
        }

        buckets = List.copyOf(buckets);
    }
}