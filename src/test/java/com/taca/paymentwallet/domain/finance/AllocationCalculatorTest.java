package com.taca.paymentwallet.domain.finance;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.*;
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
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        WalletId walletId = new WalletId(UUID.randomUUID());
        FeeConfigId feeConfigId = new FeeConfigId(UUID.randomUUID());
        TaxConfigId taxConfigId = new TaxConfigId(UUID.randomUUID());
        PaymentAllocationId allocationId = new PaymentAllocationId(UUID.randomUUID());

        PaymentOrder order = new PaymentOrder(
                orderId,
                shopId,
                Money.vnd(100_000)
        );

        List<PaymentAllocation> allocations = calculator.allocate(
                paymentId,
                List.of(order),
                Map.of(orderId, allocationId),
                Map.of(shopId, walletId),
                feeConfigId,
                taxConfigId,
                RateBps.of(700),
                RateBps.of(100)
        );

        PaymentAllocation allocation = allocations.getFirst();

        assertEquals(allocationId, allocation.id());
        assertEquals(paymentId, allocation.paymentId());
        assertEquals(walletId, allocation.walletId());
        assertEquals(feeConfigId, allocation.feeConfigId());
        assertEquals(taxConfigId, allocation.taxConfigId());
        assertEquals(orderId, allocation.orderId());
        assertEquals(shopId, allocation.shopId());
        assertEquals(Money.vnd(100_000), allocation.grossAmount());
        assertEquals(Money.vnd(7_000), allocation.commissionAmount());
        assertEquals(Money.vnd(1_000), allocation.taxAmount());
        assertEquals(Money.vnd(92_000), allocation.sellerNetAmount());
    }

    @Test
    void shouldAllocateMultipleOrders() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        OrderId orderId1 = new OrderId(UUID.randomUUID());
        OrderId orderId2 = new OrderId(UUID.randomUUID());

        ShopId shopId1 = new ShopId(UUID.randomUUID());
        ShopId shopId2 = new ShopId(UUID.randomUUID());

        WalletId walletId1 = new WalletId(UUID.randomUUID());
        WalletId walletId2 = new WalletId(UUID.randomUUID());

        PaymentAllocationId allocationId1 = new PaymentAllocationId(UUID.randomUUID());
        PaymentAllocationId allocationId2 = new PaymentAllocationId(UUID.randomUUID());

        FeeConfigId feeConfigId = new FeeConfigId(UUID.randomUUID());
        TaxConfigId taxConfigId = new TaxConfigId(UUID.randomUUID());

        PaymentOrder order1 = new PaymentOrder(
                orderId1,
                shopId1,
                Money.vnd(100_000)
        );

        PaymentOrder order2 = new PaymentOrder(
                orderId2,
                shopId2,
                Money.vnd(200_000)
        );

        List<PaymentAllocation> allocations =
                calculator.allocate(
                        paymentId,
                        List.of(order1, order2),
                        Map.of(
                                orderId1, allocationId1,
                                orderId2, allocationId2
                        ),
                        Map.of(
                                shopId1, walletId1,
                                shopId2, walletId2
                        ),
                        feeConfigId,
                        taxConfigId,
                        RateBps.of(700),
                        RateBps.of(100)
                );

        assertEquals(2, allocations.size());

        PaymentAllocation allocation1 =
                allocations.get(0);

        PaymentAllocation allocation2 =
                allocations.get(1);

        assertEquals(allocationId1, allocation1.id());
        assertEquals(paymentId, allocation1.paymentId());
        assertEquals(orderId1, allocation1.orderId());
        assertEquals(shopId1, allocation1.shopId());
        assertEquals(walletId1, allocation1.walletId());
        assertEquals(feeConfigId, allocation1.feeConfigId());
        assertEquals(taxConfigId, allocation1.taxConfigId());

        assertEquals(allocationId2, allocation2.id());
        assertEquals(paymentId, allocation2.paymentId());
        assertEquals(orderId2, allocation2.orderId());
        assertEquals(shopId2, allocation2.shopId());
        assertEquals(walletId2, allocation2.walletId());

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

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.allocate(
                        new PaymentId(UUID.randomUUID()),
                        List.of(order),
                        Map.of(),
                        Map.of(
                                order.shopId(),
                                new WalletId(UUID.randomUUID())
                        ),
                        new FeeConfigId(UUID.randomUUID()),
                        new TaxConfigId(UUID.randomUUID()),
                        RateBps.of(700),
                        RateBps.of(100)
                )
        );
    }

    @Test
    void shouldRejectMissingWalletIdForShop() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        OrderId orderId =
                new OrderId(UUID.randomUUID());

        ShopId shopId =
                new ShopId(UUID.randomUUID());

        PaymentAllocationId allocationId =
                new PaymentAllocationId(UUID.randomUUID());

        PaymentOrder order = new PaymentOrder(
                orderId,
                shopId,
                Money.vnd(100_000)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.allocate(
                        paymentId,
                        List.of(order),
                        Map.of(orderId, allocationId),
                        Map.of(),
                        new FeeConfigId(UUID.randomUUID()),
                        new TaxConfigId(UUID.randomUUID()),
                        RateBps.of(700),
                        RateBps.of(100)
                )
        );
    }
}
