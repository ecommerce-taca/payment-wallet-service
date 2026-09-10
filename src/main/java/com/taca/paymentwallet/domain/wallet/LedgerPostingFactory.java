package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LedgerPostingFactory {

    public LedgerPosting createPaymentCapturePosting(
            LedgerPostingId postingId,
            PaymentId paymentId,
            LedgerAccountId clearingAccountId,
            LedgerAccountId platformCommissionAccountId,
            LedgerAccountId taxPayableAccountId,
            Map<ShopId, LedgerAccountId> sellerPendingAccountIdsByShop,
            List<PaymentAllocation> allocations
    ) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (clearingAccountId == null) {
            throw new IllegalArgumentException("clearingAccountId must not be null");
        }

        if (platformCommissionAccountId == null) {
            throw new IllegalArgumentException("platformCommissionAccountId must not be null");
        }

        if (taxPayableAccountId == null) {
            throw new IllegalArgumentException("taxPayableAccountId must not be null");
        }

        if (sellerPendingAccountIdsByShop == null || sellerPendingAccountIdsByShop.isEmpty()) {
            throw new IllegalArgumentException("sellerPendingAccountIdsByShop must not be empty");
        }

        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("allocations must not be empty");
        }

        Money totalGross = sumGross(allocations);
        Money totalCommission = sumCommission(allocations);
        Money totalTax = sumTax(allocations);
        Map<LedgerAccountId, Money> sellerNetByAccount = groupSellerNetByAccount(
                allocations,
                sellerPendingAccountIdsByShop
        );

        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(LedgerEntry.debit(clearingAccountId, totalGross));

        if (totalCommission.isPositive()) {
            entries.add(LedgerEntry.credit(platformCommissionAccountId, totalCommission));
        }

        if (totalTax.isPositive()) {
            entries.add(LedgerEntry.credit(taxPayableAccountId, totalTax));
        }

        sellerNetByAccount.forEach((accountId, amount) -> {
            if (amount.isPositive()) {
                entries.add(LedgerEntry.credit(accountId, amount));
            }
        });

        return new LedgerPosting(
                postingId,
                "PAYMENT_CAPTURE",
                "PAYMENT_CAPTURE:" + paymentId.value(),
                "PAYMENT",
                paymentId.value().toString(),
                entries
        );
    }

    private Map<LedgerAccountId, Money> groupSellerNetByAccount(
            List<PaymentAllocation> allocations,
            Map<ShopId, LedgerAccountId> sellerPendingAccountIdsByShop
    ) {
        Map<LedgerAccountId, Money> result = new LinkedHashMap<>();

        for (PaymentAllocation allocation : allocations) {
            LedgerAccountId sellerPendingAccountId =
                    sellerPendingAccountIdsByShop.get(allocation.shopId());

            if (sellerPendingAccountId == null) {
                throw new IllegalArgumentException(
                        "missing seller pending account for shop " + allocation.shopId().value()
                );
            }

            result.merge(
                    sellerPendingAccountId,
                    allocation.sellerNetAmount(),
                    Money::add
            );
        }

        return result;
    }

    private Money sumGross(List<PaymentAllocation> allocations) {
        return allocations.stream()
                .map(PaymentAllocation::grossAmount)
                .reduce(Money.vnd(0), Money::add);
    }

    private Money sumCommission(List<PaymentAllocation> allocations) {
        return allocations.stream()
                .map(PaymentAllocation::commissionAmount)
                .reduce(Money.vnd(0), Money::add);
    }

    private Money sumTax(List<PaymentAllocation> allocations) {
        return allocations.stream()
                .map(PaymentAllocation::taxAmount)
                .reduce(Money.vnd(0), Money::add);
    }
}