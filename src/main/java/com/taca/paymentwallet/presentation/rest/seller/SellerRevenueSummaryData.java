package com.taca.paymentwallet.presentation.rest.seller;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SellerRevenueSummaryData(
        long gross,
        long commission,
        long tax,
        long net,
        long refunded,

        @JsonProperty("order_count")
        long orderCount
) {
}