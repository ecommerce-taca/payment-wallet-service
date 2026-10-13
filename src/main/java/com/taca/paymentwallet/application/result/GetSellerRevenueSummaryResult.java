package com.taca.paymentwallet.application.result;

public record GetSellerRevenueSummaryResult(
        long gross,
        long commission,
        long tax,
        long net,
        long refunded,
        long orderCount
) {
}