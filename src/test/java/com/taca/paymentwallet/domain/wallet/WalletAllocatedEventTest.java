package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.valueobject.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletAllocatedEventTest {

    @Test
    void shouldCreateWalletAllocatedEventFromAllocation() {
        WalletId walletId = new WalletId(UUID.randomUUID());

        OrderId orderId = new OrderId(UUID.randomUUID());

        ShopId shopId = new ShopId(UUID.randomUUID());

        PaymentAllocation allocation = new PaymentAllocation(
                new PaymentAllocationId(UUID.randomUUID()),
                new PaymentId(UUID.randomUUID()),
                orderId,
                shopId,
                walletId,
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(3_000),
                Money.vnd(90_000),
                new FeeConfigId(UUID.randomUUID()),
                new TaxConfigId(UUID.randomUUID())
        );

        Instant occurredAt = Instant.parse("2026-09-28T15:00:00Z");

        WalletAllocatedEvent event =
                WalletAllocatedEvent.from(
                        allocation,
                        occurredAt
                );

        assertThat(event.eventType()).isEqualTo("wallet.allocated");

        assertThat(event.aggregateId()).isEqualTo(walletId.value().toString());

        assertThat(event.occurredAt()).isEqualTo(occurredAt);

        assertThat(event.orderId()).isEqualTo(orderId);

        assertThat(event.shopId()).isEqualTo(shopId);

        assertThat(event.grossAmount()).isEqualTo(Money.vnd(100_000));

        assertThat(event.commissionAmount()).isEqualTo(Money.vnd(7_000));

        assertThat(event.taxAmount()).isEqualTo(Money.vnd(3_000));

        assertThat(event.sellerNetAmount()).isEqualTo(Money.vnd(90_000));
    }
}