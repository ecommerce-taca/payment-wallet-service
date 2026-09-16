package com.taca.paymentwallet.domain.finance;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.RateBps;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AllocationCalculatorTest {

    private final AllocationCalculator calculator = new AllocationCalculator();

    @Test
    void shouldAllocateCommissionTaxAndSellerNet() {
        OrderId orderId = new OrderId(UUID.randomUUID());
        ShopId shopId = new ShopId(UUID.randomUUID());
        PaymentAllocationId allocationId = new PaymentAllocationId(UUID.randomUUID());

        PaymentOrder order = new PaymentOrder(
                orderId,
                shopId,
                Money.vnd(100_000)
        );

        List<PaymentAllocation> allocations = calculator.allocate(
                List.of(order),
                Map.of(orderId, allocationId),
                RateBps.of(700),
                RateBps.of(100)
        );

        PaymentAllocation allocation = allocations.getFirst();

        assertEquals(allocationId, allocation.id());
        assertEquals(orderId, allocation.orderId());
        assertEquals(shopId, allocation.shopId());
        assertEquals(Money.vnd(100_000), allocation.grossAmount());
        assertEquals(Money.vnd(7_000), allocation.commissionAmount());
        assertEquals(Money.vnd(1_000), allocation.taxAmount());
        assertEquals(Money.vnd(92_000), allocation.sellerNetAmount());
    }

    @Test
    void shouldAllocateMultipleOrders() {
        OrderId orderId1 = new OrderId(UUID.randomUUID());
        OrderId orderId2 = new OrderId(UUID.randomUUID());
        PaymentAllocationId allocationId1 = new PaymentAllocationId(UUID.randomUUID());
        PaymentAllocationId allocationId2 = new PaymentAllocationId(UUID.randomUUID());
        PaymentOrder order1 = new PaymentOrder(
                orderId1,
                new ShopId(UUID.randomUUID()),
                Money.vnd(100_000)
        );

        PaymentOrder order2 = new PaymentOrder(
                orderId2,
                new ShopId(UUID.randomUUID()),
                Money.vnd(200_000)
        );

        List<PaymentAllocation> allocations = calculator.allocate(
                List.of(order1, order2),
                Map.of(
                        orderId1, allocationId1,
                        orderId2, allocationId2
                ),
                RateBps.of(700),
                RateBps.of(100)
        );

        assertEquals(2, allocations.size());
        assertEquals(allocationId1, allocations.get(0).id());
        assertEquals(allocationId2, allocations.get(1).id());

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

    @Test
    void shouldRejectMissingAllocationIdForOrder() {
        PaymentOrder order = new PaymentOrder(
                new OrderId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                Money.vnd(100_000)
        );

        assertThrows(IllegalArgumentException.class, () -> calculator.allocate(
                List.of(order),
                Map.of(),
                RateBps.of(700),
                RateBps.of(100)
        ));
    }
}
