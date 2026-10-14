package com.taca.paymentwallet.presentation.rest.seller;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SellerRevenueBucketData(
        String period,
        long gross,
        long commission,
        long tax,
        long net,

        @JsonProperty("order_count")
        long orderCount
) {
}