package com.taca.paymentwallet.application.fee;

import com.taca.paymentwallet.domain.valueobject.FeeConfigId;
import com.taca.paymentwallet.domain.valueobject.RateBps;
import com.taca.paymentwallet.domain.valueobject.TaxConfigId;

public record PaymentFeePolicy(
        FeeConfigId feeConfigId,
        TaxConfigId taxConfigId,
        RateBps commissionRate,
        RateBps taxRate
) {

    public PaymentFeePolicy {
        if (feeConfigId == null) {
            throw new IllegalArgumentException("feeConfigId must not be null");
        }

        if (taxConfigId == null) {
            throw new IllegalArgumentException("taxConfigId must not be null");
        }

        if (commissionRate == null) {
            throw new IllegalArgumentException("commissionRate must not be null");
        }

        if (taxRate == null) {
            throw new IllegalArgumentException("taxRate must not be null");
        }
    }
}