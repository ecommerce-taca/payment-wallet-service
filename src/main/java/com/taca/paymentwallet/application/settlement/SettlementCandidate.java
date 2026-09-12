package com.taca.paymentwallet.application.settlement;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

public record SettlementCandidate(
        PaymentAllocationId paymentAllocationId,
        ShopId shopId,
        WalletId walletId,
        Money grossAmount,
        Money commissionAmount,
        Money taxAmount,
        Money sellerNetAmount,
        Money releasableAmount,
        Money heldAmount
) {

    public SettlementCandidate {
        if (paymentAllocationId == null) {
            throw new IllegalArgumentException("paymentAllocationId must not be null");
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

        if (sellerNetAmount == null || !sellerNetAmount.isPositive()) {
            throw new IllegalArgumentException("sellerNetAmount must be positive");
        }

        if (releasableAmount == null || !releasableAmount.isPositive()) {
            throw new IllegalArgumentException("releasableAmount must be positive");
        }

        if (heldAmount == null) {
            throw new IllegalArgumentException("heldAmount must not be null");
        }

        if (!commissionAmount.add(taxAmount).add(sellerNetAmount).equals(grossAmount)) {
            throw new IllegalArgumentException("grossAmount must equal commissionAmount + taxAmount + sellerNetAmount");
        }

        if (!releasableAmount.add(heldAmount).equals(sellerNetAmount)) {
            throw new IllegalArgumentException("sellerNetAmount must equal releasableAmount + heldAmount");
        }
    }
}