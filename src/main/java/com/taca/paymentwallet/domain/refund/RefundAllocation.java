package com.taca.paymentwallet.domain.refund;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;

public record RefundAllocation(
        PaymentAllocationId paymentAllocationId,
        Money grossAmount,
        Money commissionReversal,
        Money taxReversal,
        Money sellerReversal
) {

    public RefundAllocation {
        if (paymentAllocationId == null) {
            throw new IllegalArgumentException("paymentAllocationId must not be null");
        }

        if (grossAmount == null || !grossAmount.isPositive()) {
            throw new IllegalArgumentException("grossAmount must be positive");
        }

        if (commissionReversal == null) {
            throw new IllegalArgumentException("commissionReversal must not be null");
        }

        if (taxReversal == null) {
            throw new IllegalArgumentException("taxReversal must not be null");
        }

        if (sellerReversal == null) {
            throw new IllegalArgumentException("sellerReversal must not be null");
        }

        Money total = commissionReversal.add(taxReversal).add(sellerReversal);

        if (!total.equals(grossAmount)) {
            throw new IllegalArgumentException(
                    "grossAmount must equal commissionReversal + taxReversal + sellerReversal"
            );
        }
    }
}
