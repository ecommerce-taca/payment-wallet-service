package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

public record PaymentAllocation(
        PaymentAllocationId id,
        OrderId orderId,
        ShopId shopId,
        Money grossAmount,
        Money commissionAmount,
        Money taxAmount,
        Money sellerNetAmount
) {

    public PaymentAllocation {
        if (id == null) {
            throw new IllegalArgumentException("paymentAllocationId must not be null");
        }

        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (grossAmount == null || !grossAmount.isPositive()) {
            throw new IllegalArgumentException("grossAmount must be positive");
        }

        if (commissionAmount == null) {
            throw new IllegalArgumentException("commissionAmount must not be null");
        }

        if (taxAmount == null) {
            throw new IllegalArgumentException("taxAmount must not be null");
        }

        if (sellerNetAmount == null) {
            throw new IllegalArgumentException("sellerNetAmount must not be null");
        }

        Money total = commissionAmount.add(taxAmount).add(sellerNetAmount);

        if (!total.equals(grossAmount)) {
            throw new IllegalArgumentException("grossAmount must equal commissionAmount + taxAmount + sellerNetAmount");
        }
    }
}
