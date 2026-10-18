package com.taca.paymentwallet.application.result;

public record GetSellerRevenueBucketResult(
        String period,
        long gross,
        long commission,
        long tax,
        long net,
        long orderCount
) {
}