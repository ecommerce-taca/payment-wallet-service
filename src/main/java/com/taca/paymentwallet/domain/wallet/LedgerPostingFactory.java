package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.util.ArrayList;
import java.util.List;

public class LedgerPostingFactory {

    public LedgerPosting createPaymentCapturePosting(
            LedgerPostingId postingId,
            PaymentId paymentId,
            LedgerAccountId clearingAccountId,
            LedgerAccountId platformCommissionAccountId,
            LedgerAccountId taxPayableAccountId,
            LedgerAccountId sellerPendingAccountId,
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

        if (sellerPendingAccountId == null) {
            throw new IllegalArgumentException("sellerPendingAccountId must not be null");
        }

        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("allocations must not be empty");
        }

        Money totalGross = sumGross(allocations);
        Money totalCommission = sumCommission(allocations);
        Money totalTax = sumTax(allocations);
        Money totalSellerNet = sumSellerNet(allocations);

        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(LedgerEntry.debit(clearingAccountId, totalGross));

        if (totalCommission.isPositive()) {
            entries.add(LedgerEntry.credit(platformCommissionAccountId, totalCommission));
        }

        if (totalTax.isPositive()) {
            entries.add(LedgerEntry.credit(taxPayableAccountId, totalTax));
        }

        if (totalSellerNet.isPositive()) {
            entries.add(LedgerEntry.credit(sellerPendingAccountId, totalSellerNet));
        }

        return new LedgerPosting(
                postingId,
                "PAYMENT_CAPTURE",
                "PAYMENT_CAPTURE:" + paymentId.value(),
                "PAYMENT",
                paymentId.value().toString(),
                entries
        );
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

    private Money sumSellerNet(List<PaymentAllocation> allocations) {
        return allocations.stream()
                .map(PaymentAllocation::sellerNetAmount)
                .reduce(Money.vnd(0), Money::add);
    }
}