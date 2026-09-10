package com.taca.paymentwallet.domain.finance;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.RateBps;

import java.util.List;
import java.util.Map;

public class AllocationCalculator {

    public List<PaymentAllocation> allocate(
            List<PaymentOrder> orders,
            Map<OrderId, PaymentAllocationId> allocationIdsByOrder,
            RateBps commissionRate,
            RateBps taxRate
    ) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("orders must not be empty");
        }

        if (allocationIdsByOrder == null || allocationIdsByOrder.isEmpty()) {
            throw new IllegalArgumentException("allocationIdsByOrder must not be empty");
        }

        if (commissionRate == null) {
            throw new IllegalArgumentException("commissionRate must not be null");
        }

        if (taxRate == null) {
            throw new IllegalArgumentException("taxRate must not be null");
        }

        return orders.stream()
                .map(order -> allocateOrder(
                        order,
                        allocationIdOf(order, allocationIdsByOrder),
                        commissionRate,
                        taxRate
                ))
                .toList();
    }

    private PaymentAllocationId allocationIdOf(
            PaymentOrder order,
            Map<OrderId, PaymentAllocationId> allocationIdsByOrder
    ) {
        PaymentAllocationId allocationId = allocationIdsByOrder.get(order.orderId());

        if (allocationId == null) {
            throw new IllegalArgumentException(
                    "missing payment allocation id for order " + order.orderId().value()
            );
        }

        return allocationId;
    }

    private PaymentAllocation allocateOrder(
            PaymentOrder order,
            PaymentAllocationId allocationId,
            RateBps commissionRate,
            RateBps taxRate
    ) {
        Money gross = order.amount();
        Money commission = gross.multiplyBy(commissionRate);
        Money tax = gross.multiplyBy(taxRate);
        Money sellerNet = gross.subtract(commission).subtract(tax);

        return new PaymentAllocation(
                allocationId,
                order.orderId(),
                order.shopId(),
                gross,
                commission,
                tax,
                sellerNet
        );
    }
}