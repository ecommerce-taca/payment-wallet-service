package com.taca.paymentwallet.domain.finance;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.FeeConfigId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RateBps;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.TaxConfigId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

import java.util.List;
import java.util.Map;

public class AllocationCalculator {

    public List<PaymentAllocation> allocate(
            PaymentId paymentId,
            List<PaymentOrder> orders,
            Map<OrderId, PaymentAllocationId> allocationIdsByOrder,
            Map<ShopId, WalletId> walletIdsByShop,
            FeeConfigId feeConfigId,
            TaxConfigId taxConfigId,
            RateBps commissionRate,
            RateBps taxRate
    ) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("orders must not be empty");
        }

        if (allocationIdsByOrder == null || allocationIdsByOrder.isEmpty()) {
            throw new IllegalArgumentException("allocationIdsByOrder must not be empty");
        }

        if (walletIdsByShop == null || walletIdsByShop.isEmpty()) {
            throw new IllegalArgumentException("walletIdsByShop must not be empty");
        }

        if (feeConfigId == null) {
            throw new IllegalArgumentException("feeConfigId must not be null");
        }

        if (taxConfigId == null) {
            throw new IllegalArgumentException("taxConfigId must not be null");
        }

        if (commissionRate == null) {
            throw new IllegalArgumentException("commissionRate must not be null");
        }

        if (taxRate == null) {
            throw new IllegalArgumentException("taxRate must not be null");
        }

        return orders.stream()
                .map(order -> allocateOrder(
                        paymentId,
                        order,
                        allocationIdOf(order, allocationIdsByOrder),
                        walletIdOf(order, walletIdsByShop),
                        feeConfigId,
                        taxConfigId,
                        commissionRate,
                        taxRate
                ))
                .toList();
    }

    private PaymentAllocationId allocationIdOf(
            PaymentOrder order,
            Map<OrderId, PaymentAllocationId> allocationIdsByOrder
    ) {
        PaymentAllocationId allocationId =
                allocationIdsByOrder.get(order.orderId());

        if (allocationId == null) {
            throw new IllegalArgumentException(
                    "missing payment allocation id for order "
                            + order.orderId().value()
            );
        }

        return allocationId;
    }

    private WalletId walletIdOf(
            PaymentOrder order,
            Map<ShopId, WalletId> walletIdsByShop
    ) {
        WalletId walletId = walletIdsByShop.get(order.shopId());

        if (walletId == null) {
            throw new IllegalArgumentException(
                    "missing wallet id for shop "
                            + order.shopId().value()
            );
        }

        return walletId;
    }

    private PaymentAllocation allocateOrder(
            PaymentId paymentId,
            PaymentOrder order,
            PaymentAllocationId allocationId,
            WalletId walletId,
            FeeConfigId feeConfigId,
            TaxConfigId taxConfigId,
            RateBps commissionRate,
            RateBps taxRate
    ) {
        Money gross = order.amount();
        Money commission = gross.multiplyBy(commissionRate);
        Money tax = gross.multiplyBy(taxRate);
        Money sellerNet = gross
                .subtract(commission)
                .subtract(tax);

        return new PaymentAllocation(
                allocationId,
                paymentId,
                order.orderId(),
                order.shopId(),
                walletId,
                gross,
                commission,
                tax,
                sellerNet,
                feeConfigId,
                taxConfigId
        );
    }
}