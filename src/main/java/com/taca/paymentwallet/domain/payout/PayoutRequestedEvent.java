package com.taca.paymentwallet.domain.payout;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PayoutRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        PayoutId payoutId,
        ShopId shopId,
        Money amount
) implements DomainEvent {

    public PayoutRequestedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(payoutId, "payoutId must not be null");
        Objects.requireNonNull(shopId, "shopId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }

    public static PayoutRequestedEvent now(
            PayoutId payoutId,
            ShopId shopId,
            Money amount
    ) {
        return new PayoutRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                payoutId,
                shopId,
                amount
        );
    }

    @Override
    public String aggregateId() {
        return payoutId.value().toString();
    }

    @Override
    public String eventType() {
        return "payout.requested";
    }
}
