package com.taca.paymentwallet.domain.finance;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.RateBps;

import java.util.List;

public class AllocationCalculator {

    public List<PaymentAllocation> allocate(
            List<PaymentOrder> orders,
            RateBps commissionRate,
            RateBps taxRate
    ) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("orders must not be empty");
        }

        if (commissionRate == null) {
            throw new IllegalArgumentException("commissionRate must not be null");
        }

        if (taxRate == null) {
            throw new IllegalArgumentException("taxRate must not be null");
        }

        return orders.stream()
                .map(order -> allocateOrder(order, commissionRate, taxRate))
                .toList();
    }

    private PaymentAllocation allocateOrder(
            PaymentOrder order,
            RateBps commissionRate,
            RateBps taxRate
    ) {
        Money gross = order.amount();
        Money commission = gross.multiplyBy(commissionRate);
        Money tax = gross.multiplyBy(taxRate);
        Money sellerNet = gross.subtract(commission).subtract(tax);

        return new PaymentAllocation(
                order.orderId(),
                order.shopId(),
                gross,
                commission,
                tax,
                sellerNet
        );
    }
}
