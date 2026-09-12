package com.taca.paymentwallet.application.fee;

import com.taca.paymentwallet.domain.valueobject.RateBps;

public record PaymentFeePolicy(
        RateBps commissionRate,
        RateBps taxRate
) {

    public PaymentFeePolicy {
        if (commissionRate == null) {
            throw new IllegalArgumentException("commissionRate must not be null");
        }
        if (taxRate == null) {
            throw new IllegalArgumentException("taxRate must not be null");
        }
    }
}
