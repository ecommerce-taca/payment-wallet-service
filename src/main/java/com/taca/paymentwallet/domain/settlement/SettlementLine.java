package com.taca.paymentwallet.domain.settlement;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.SettlementLineId;

public record SettlementLine(
        SettlementLineId id,
        PaymentAllocationId paymentAllocationId,
        Money releasedAmount
) {

    public SettlementLine {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (paymentAllocationId == null) {
            throw new IllegalArgumentException("paymentAllocationId must not be null");
        }

        if (releasedAmount == null || !releasedAmount.isPositive()) {
            throw new IllegalArgumentException("releasedAmount must be positive");
        }
    }
}
