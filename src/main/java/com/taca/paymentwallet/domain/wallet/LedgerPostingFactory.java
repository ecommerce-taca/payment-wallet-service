package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.valueobject.*;

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

    public LedgerPosting createSettlementReleasePosting(
            LedgerPostingId postingId,
            SettlementBatchItemId settlementBatchItemId,
            LedgerAccountId sellerPendingAccountId,
            LedgerAccountId sellerAvailableAccountId,
            Money releasedAmount
    ) {
        require(postingId, "postingId");
        require(settlementBatchItemId, "settlementBatchItemId");
        require(sellerPendingAccountId, "sellerPendingAccountId");
        require(sellerAvailableAccountId, "sellerAvailableAccountId");
        requirePositiveMoney(releasedAmount, "releasedAmount");

        return new LedgerPosting(
                postingId,
                "SETTLEMENT_RELEASE",
                "SETTLEMENT_RELEASE:" + settlementBatchItemId.value(),
                "SETTLEMENT_BATCH_ITEM",
                settlementBatchItemId.value().toString(),
                List.of(
                        LedgerEntry.debit(sellerPendingAccountId, releasedAmount),
                        LedgerEntry.credit(sellerAvailableAccountId, releasedAmount)
                )
        );
    }

    public LedgerPosting createPayoutReservePosting(
            LedgerPostingId postingId,
            PayoutId payoutId,
            LedgerAccountId sellerAvailableAccountId,
            LedgerAccountId payoutClearingAccountId,
            Money amount
    ) {
        require(postingId, "postingId");
        require(payoutId, "payoutId");
        require(sellerAvailableAccountId, "sellerAvailableAccountId");
        require(payoutClearingAccountId, "payoutClearingAccountId");
        requirePositiveMoney(amount, "amount");

        return new LedgerPosting(
                postingId,
                "PAYOUT_RESERVE",
                "PAYOUT_RESERVE:" + payoutId.value(),
                "PAYOUT",
                payoutId.value().toString(),
                List.of(
                        LedgerEntry.debit(sellerAvailableAccountId, amount),
                        LedgerEntry.credit(payoutClearingAccountId, amount)
                )
        );
    }

    public LedgerPosting createPayoutReversalPosting(
            LedgerPostingId postingId,
            PayoutId payoutId,
            LedgerAccountId payoutClearingAccountId,
            LedgerAccountId sellerAvailableAccountId,
            Money amount
    ) {
        require(postingId, "postingId");
        require(payoutId, "payoutId");
        require(payoutClearingAccountId, "payoutClearingAccountId");
        require(sellerAvailableAccountId, "sellerAvailableAccountId");
        requirePositiveMoney(amount, "amount");

        return new LedgerPosting(
                postingId,
                "PAYOUT_REVERSAL",
                "PAYOUT_REVERSAL:" + payoutId.value(),
                "PAYOUT",
                payoutId.value().toString(),
                List.of(
                        LedgerEntry.debit(payoutClearingAccountId, amount),
                        LedgerEntry.credit(sellerAvailableAccountId, amount)
                )
        );
    }

    public LedgerPosting createRefundSuccessPosting(
            LedgerPostingId postingId,
            RefundId refundId,
            LedgerAccountId refundClearingAccountId,
            LedgerAccountId platformCommissionAccountId,
            LedgerAccountId taxPayableAccountId,
            Map<PaymentAllocationId, LedgerAccountId> sellerAccountIdsByAllocation,
            List<RefundAllocation> refundAllocations
    ) {
        require(postingId, "postingId");
        require(refundId, "refundId");
        require(refundClearingAccountId, "refundClearingAccountId");
        require(platformCommissionAccountId, "platformCommissionAccountId");
        require(taxPayableAccountId, "taxPayableAccountId");

        if (sellerAccountIdsByAllocation == null
                || sellerAccountIdsByAllocation.isEmpty()) {
            throw new IllegalArgumentException("sellerAccountIdsByAllocation must not be empty");
        }

        if (refundAllocations == null || refundAllocations.isEmpty()) {
            throw new IllegalArgumentException("refundAllocations must not be empty");
        }

        Money totalGross = refundAllocations.stream()
                .map(RefundAllocation::grossAmount)
                .reduce(Money.vnd(0), Money::add);

        Money totalCommission = refundAllocations.stream()
                .map(RefundAllocation::commissionReversal)
                .reduce(Money.vnd(0), Money::add);

        Money totalTax = refundAllocations.stream()
                .map(RefundAllocation::taxReversal)
                .reduce(Money.vnd(0), Money::add);

        List<LedgerEntry> entries = new ArrayList<>();

        if (totalCommission.isPositive()) {
            entries.add(LedgerEntry.debit(platformCommissionAccountId, totalCommission));
        }

        if (totalTax.isPositive()) {
            entries.add(LedgerEntry.debit(taxPayableAccountId, totalTax));
        }

        for (RefundAllocation allocation : refundAllocations) {
            LedgerAccountId sellerAccountId =
                    sellerAccountIdsByAllocation.get(allocation.paymentAllocationId());

            if (sellerAccountId == null) {
                throw new IllegalArgumentException(
                        "missing seller account for allocation " + allocation.paymentAllocationId().value()
                );
            }

            if (allocation.sellerReversal().isPositive()) {
                entries.add(LedgerEntry.debit(sellerAccountId, allocation.sellerReversal()));
            }
        }

        entries.add(LedgerEntry.credit(refundClearingAccountId, totalGross));

        return new LedgerPosting(
                postingId,
                "REFUND_SUCCESS",
                "REFUND_SUCCESS:" + refundId.value(),
                "REFUND",
                refundId.value().toString(),
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

    private void require(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private void requirePositiveMoney(Money money, String fieldName) {
        if (money == null || !money.isPositive()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}