package com.taca.paymentwallet.application.settlement;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

import java.time.Instant;
import java.util.Objects;

public record UnsettledSettlementCandidate(
        PaymentAllocationId paymentAllocationId,
        ShopId shopId,
        WalletId walletId,
        Money grossAmount,
        Money commissionAmount,
        Money taxAmount,
        Money sellerNetAmount,
        Instant allocationCreatedAt
) {

    public UnsettledSettlementCandidate {
        Objects.requireNonNull(
                paymentAllocationId,
                "paymentAllocationId must not be null"
        );

        Objects.requireNonNull(
                shopId,
                "shopId must not be null"
        );

        Objects.requireNonNull(
                walletId,
                "walletId must not be null"
        );

        Objects.requireNonNull(
                grossAmount,
                "grossAmount must not be null"
        );

        Objects.requireNonNull(
                commissionAmount,
                "commissionAmount must not be null"
        );

        Objects.requireNonNull(
                taxAmount,
                "taxAmount must not be null"
        );

        Objects.requireNonNull(
                sellerNetAmount,
                "sellerNetAmount must not be null"
        );

        Objects.requireNonNull(
                allocationCreatedAt,
                "allocationCreatedAt must not be null"
        );

        if (!grossAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "grossAmount must be positive"
            );
        }

        if (!sellerNetAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "sellerNetAmount must be positive"
            );
        }

        if (!commissionAmount
                .add(taxAmount)
                .add(sellerNetAmount)
                .equals(grossAmount)) {

            throw new IllegalArgumentException(
                    "grossAmount must equal commissionAmount + taxAmount + sellerNetAmount"
            );
        }
    }
}