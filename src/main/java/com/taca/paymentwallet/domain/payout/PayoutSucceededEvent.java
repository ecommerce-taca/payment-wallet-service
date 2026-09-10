package com.taca.paymentwallet.domain.payout;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.time.Instant;
import java.util.UUID;

public record PayoutSucceededEvent(
        UUID eventId,
        Instant occurredAt,
        PayoutId payoutId,
        ShopId shopId,
        Money amount,
        String providerReference
) implements DomainEvent {

    public PayoutSucceededEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }
        
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        if (payoutId == null) {
            throw new IllegalArgumentException("Payout id must not be null");
        }

        if (shopId == null) {
            throw new IllegalArgumentException("Shop id must not be null");
        }

        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }

        if (providerReference == null || providerReference.isBlank()) {
            throw new IllegalArgumentException("Provider reference must not be blank");
        }
    }

    public static PayoutSucceededEvent now(
            PayoutId payoutId,
            ShopId shopId,
            Money amount,
            String providerReference
    ) {
        return new PayoutSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                payoutId,
                shopId,
                amount,
                providerReference
        );
    }

    @Override
    public String aggregateId() {
        return payoutId.value().toString();
    }

    @Override
    public String eventType() {
        return "payout.succeeded";
    }
}