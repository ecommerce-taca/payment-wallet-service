package com.taca.paymentwallet.domain.wallet;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WalletAllocatedEvent(
        UUID eventId,
        Instant occurredAt,
        WalletId walletId,
        OrderId orderId,
        ShopId shopId,
        Money grossAmount,
        Money commissionAmount,
        Money taxAmount,
        Money sellerNetAmount
) implements DomainEvent {

    public WalletAllocatedEvent {
        Objects.requireNonNull(
                eventId,
                "eventId must not be null"
        );

        Objects.requireNonNull(
                occurredAt,
                "occurredAt must not be null"
        );

        Objects.requireNonNull(
                walletId,
                "walletId must not be null"
        );

        Objects.requireNonNull(
                orderId,
                "orderId must not be null"
        );

        Objects.requireNonNull(
                shopId,
                "shopId must not be null"
        );

        Objects.requireNonNull(
                grossAmount,
                "grossAmount must not be null"
        );

        Objects.requireNonNull(
                commissionAmount,
                "commissionAmount must not be null"
        );

        Objects.requireNonNull(
                taxAmount,
                "taxAmount must not be null"
        );

        Objects.requireNonNull(
                sellerNetAmount,
                "sellerNetAmount must not be null"
        );
    }

    public static WalletAllocatedEvent from(
            PaymentAllocation allocation,
            Instant occurredAt
    ) {
        Objects.requireNonNull(
                allocation,
                "allocation must not be null"
        );

        return new WalletAllocatedEvent(
                UUID.randomUUID(),
                occurredAt,
                allocation.walletId(),
                allocation.orderId(),
                allocation.shopId(),
                allocation.grossAmount(),
                allocation.commissionAmount(),
                allocation.taxAmount(),
                allocation.sellerNetAmount()
        );
    }

    @Override
    public String aggregateId() {
        return walletId.value().toString();
    }

    @Override
    public String eventType() {
        return "wallet.allocated";
    }
}