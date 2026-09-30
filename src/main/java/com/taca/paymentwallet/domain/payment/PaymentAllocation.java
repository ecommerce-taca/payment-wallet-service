package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.valueobject.FeeConfigId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.TaxConfigId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

public record PaymentAllocation(
        PaymentAllocationId id,
        PaymentId paymentId,
        OrderId orderId,
        ShopId shopId,
        WalletId walletId,
        Money grossAmount,
        Money commissionAmount,
        Money taxAmount,
        Money sellerNetAmount,
        FeeConfigId feeConfigId,
        TaxConfigId taxConfigId
) {

    public PaymentAllocation {
        if (id == null) {
            throw new IllegalArgumentException("paymentAllocationId must not be null");
        }

        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("shopId must not be null");
        }

        if (walletId == null) {
            throw new IllegalArgumentException("walletId must not be null");
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

        if (feeConfigId == null) {
            throw new IllegalArgumentException("feeConfigId must not be null");
        }

        if (taxConfigId == null) {
            throw new IllegalArgumentException("taxConfigId must not be null");
        }

        Money total = commissionAmount
                .add(taxAmount)
                .add(sellerNetAmount);

        if (!total.equals(grossAmount)) {
            throw new IllegalArgumentException(
                    "grossAmount must equal commissionAmount + taxAmount + sellerNetAmount"
            );
        }
    }
}