package com.taca.paymentwallet.domain.finance;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.valueobject.Money;

import java.util.ArrayList;
import java.util.List;

public class RefundAllocationCalculator {

    public List<RefundAllocation> allocate(
            Money refundAmount,
            List<PaymentAllocation> paymentAllocations
    ) {
        if (refundAmount == null || !refundAmount.isPositive()) {
            throw new IllegalArgumentException("refundAmount must be positive");
        }
        
        if (paymentAllocations == null || paymentAllocations.isEmpty()) {
            throw new IllegalArgumentException("paymentAllocations must not be empty");
        }

        Money totalGross = paymentAllocations.stream()
                .map(PaymentAllocation::grossAmount)
                .reduce(Money.vnd(0), Money::add);

        if (refundAmount.isGreaterThan(totalGross)) {
            throw new IllegalArgumentException("refundAmount must not exceed total allocation gross amount");
        }

        List<RefundAllocation> result = new ArrayList<>();
        long remaining = refundAmount.amount();

        for (PaymentAllocation allocation : paymentAllocations) {
            if (remaining == 0) {
                break;
            }

            long currentGrossAmount = Math.min(remaining, allocation.grossAmount().amount());
            Money currentGross = Money.vnd(currentGrossAmount);

            Money commissionReversal = reverseComponent(
                    currentGross,
                    allocation.commissionAmount(),
                    allocation.grossAmount()
            );

            Money taxReversal = reverseComponent(
                    currentGross,
                    allocation.taxAmount(),
                    allocation.grossAmount()
            );

            Money sellerReversal = currentGross
                    .subtract(commissionReversal)
                    .subtract(taxReversal);

            result.add(new RefundAllocation(
                    allocation.id(),
                    currentGross,
                    commissionReversal,
                    taxReversal,
                    sellerReversal
            ));

            remaining -= currentGrossAmount;
        }

        return List.copyOf(result);
    }

    private Money reverseComponent(
            Money refundPart,
            Money originalComponent,
            Money originalGross
    ) {
        if (refundPart.equals(originalGross)) {
            return originalComponent;
        }

        long amount = Math.round(
                (double) refundPart.amount()
                        * originalComponent.amount()
                        / originalGross.amount()
        );

        return Money.vnd(amount);
    }
}
