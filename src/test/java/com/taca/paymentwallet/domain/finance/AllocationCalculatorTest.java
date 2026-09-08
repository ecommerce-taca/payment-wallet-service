package com.taca.paymentwallet.domain.finance;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.RateBps;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllocationCalculatorTest {

    private final AllocationCalculator calculator = new AllocationCalculator();

    @Test
    void shouldAllocateCommissionTaxAndSellerNet() {
        PaymentOrder order = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                Money.vnd(100_000)
        );

        List<PaymentAllocation> allocations = calculator.allocate(
                List.of(order),
                RateBps.of(700),
                RateBps.of(100)
        );

        PaymentAllocation allocation = allocations.getFirst();

        assertEquals(Money.vnd(100_000), allocation.grossAmount());
        assertEquals(Money.vnd(7_000), allocation.commissionAmount());
        assertEquals(Money.vnd(1_000), allocation.taxAmount());
        assertEquals(Money.vnd(92_000), allocation.sellerNetAmount());
    }

    @Test
    void shouldAllocateMultipleOrders() {
        PaymentOrder order1 = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                Money.vnd(100_000)
        );

        PaymentOrder order2 = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                Money.vnd(200_000)
        );

        List<PaymentAllocation> allocations = calculator.allocate(
                List.of(order1, order2),
                RateBps.of(700),
                RateBps.of(100)
        );

        assertEquals(2, allocations.size());

        long totalGross = allocations.stream()
                .map(PaymentAllocation::grossAmount)
                .mapToLong(Money::amount)
                .sum();

        long totalSellerNet = allocations.stream()
                .map(PaymentAllocation::sellerNetAmount)
                .mapToLong(Money::amount)
                .sum();

        assertEquals(300_000, totalGross);
        assertEquals(276_000, totalSellerNet);
    }
}