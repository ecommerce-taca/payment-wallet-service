package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LedgerPostingFactoryTest {

    private final LedgerPostingFactory factory = new LedgerPostingFactory();

    @Test
    void shouldCreatePaymentCapturePostingFromAllocations() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        OrderId orderId = new OrderId(UUID.randomUUID());
        ShopId shopId = new ShopId(UUID.randomUUID());
        PaymentAllocationId id = new PaymentAllocationId(UUID.randomUUID());
        LedgerAccountId clearingAccountId = accountId();
        LedgerAccountId platformCommissionAccountId = accountId();
        LedgerAccountId taxPayableAccountId = accountId();
        LedgerAccountId sellerPendingAccountId = accountId();
        LedgerAccountId shipmentPayableAccountId = accountId();

        PaymentAllocation allocation = new PaymentAllocation(
                id,
                paymentId,
                orderId,
                shopId,
                new WalletId(UUID.randomUUID()),
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000),
                new FeeConfigId(UUID.randomUUID()),
                new TaxConfigId(UUID.randomUUID())
        );

        Map<ShopId, LedgerAccountId> sellerPendingAccountIdsByShop = Map.of(
                shopId, sellerPendingAccountId
        );

        LedgerPosting posting = factory.createPaymentCapturePosting(
                new LedgerPostingId(UUID.randomUUID()),
                paymentId,
                clearingAccountId,
                platformCommissionAccountId,
                taxPayableAccountId,
                shipmentPayableAccountId,
                sellerPendingAccountIdsByShop,
                List.of(allocation),
                Money.vnd(20_000)
        );

        assertEquals("PAYMENT_CAPTURE", posting.postingType());
        assertEquals("PAYMENT_CAPTURE:" + paymentId.value(), posting.businessKey());
        assertEquals(5, posting.entries().size());

        assertEquals(new LedgerEntry(clearingAccountId, LedgerEntryType.DEBIT, Money.vnd(120_000)), posting.entries().get(0));
        assertEquals(new LedgerEntry(platformCommissionAccountId, LedgerEntryType.CREDIT, Money.vnd(7_000)), posting.entries().get(1));
        assertEquals(new LedgerEntry(taxPayableAccountId, LedgerEntryType.CREDIT, Money.vnd(1_000)), posting.entries().get(2));
        assertEquals(new LedgerEntry(sellerPendingAccountId, LedgerEntryType.CREDIT, Money.vnd(92_000)), posting.entries().get(3));
        assertEquals(new LedgerEntry(shipmentPayableAccountId, LedgerEntryType.CREDIT, Money.vnd(20_000)), posting.entries().get(4));
    }

    @Test
    void shouldCreateSettlementReleasePosting() {
        LedgerPosting posting = factory.createSettlementReleasePosting(
                new LedgerPostingId(UUID.randomUUID()),
                new SettlementBatchItemId(UUID.randomUUID()),
                new LedgerAccountId(UUID.randomUUID()),
                new LedgerAccountId(UUID.randomUUID()),
                Money.vnd(100_000)
        );

        assertThat(posting.postingType()).isEqualTo("SETTLEMENT_RELEASE");
        assertThat(posting.entries()).hasSize(2);
    }

    @Test
    void shouldCreatePayoutReversalPosting() {
        LedgerPosting posting = factory.createPayoutReversalPosting(
                new LedgerPostingId(UUID.randomUUID()),
                new PayoutId(UUID.randomUUID()),
                new LedgerAccountId(UUID.randomUUID()),
                new LedgerAccountId(UUID.randomUUID()),
                Money.vnd(50_000)
        );

        assertThat(posting.postingType()).isEqualTo("PAYOUT_REVERSAL");
        assertThat(posting.entries()).hasSize(2);
    }

    @Test
    void shouldNotCreateShipmentEntryWhenShippingFeeIsZero() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        ShopId shopId = new ShopId(UUID.randomUUID());

        LedgerAccountId clearingAccountId = accountId();

        LedgerAccountId platformCommissionAccountId = accountId();

        LedgerAccountId taxPayableAccountId = accountId();

        LedgerAccountId shipmentPayableAccountId = accountId();

        LedgerAccountId sellerPendingAccountId = accountId();

        PaymentAllocation allocation = new PaymentAllocation(
                new PaymentAllocationId(UUID.randomUUID()),
                paymentId,
                new OrderId(UUID.randomUUID()),
                shopId,
                new WalletId(UUID.randomUUID()),
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000),
                new FeeConfigId(UUID.randomUUID()),
                new TaxConfigId(UUID.randomUUID())
        );

        LedgerPosting posting = factory.createPaymentCapturePosting(
                new LedgerPostingId(UUID.randomUUID()),
                paymentId,
                clearingAccountId,
                platformCommissionAccountId,
                taxPayableAccountId,
                shipmentPayableAccountId,
                Map.of(
                        shopId,
                        sellerPendingAccountId
                ),
                List.of(allocation),
                Money.vnd(0)
        );

        assertThat(posting.entries()).hasSize(4);

        assertThat(
                posting.entries()
                        .stream()
                        .noneMatch(
                                entry ->
                                        entry.accountId()
                                                .equals(shipmentPayableAccountId)
                        )
        ).isTrue();
    }

    private LedgerAccountId accountId() {
        return new LedgerAccountId(UUID.randomUUID());
    }
}
