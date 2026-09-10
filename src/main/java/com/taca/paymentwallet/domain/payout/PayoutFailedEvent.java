package com.taca.paymentwallet.domain.payout;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PayoutFailedEvent(
        UUID eventId,
        Instant occurredAt,
        PayoutId payoutId,
        ShopId shopId,
        String failureCode
) implements DomainEvent {

    public PayoutFailedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(payoutId, "payoutId must not be null");
        Objects.requireNonNull(shopId, "shopId must not be null");

        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }
    }

    public static PayoutFailedEvent now(
            PayoutId payoutId,
            ShopId shopId,
            String failureCode
    ) {
        return new PayoutFailedEvent(
                UUID.randomUUID(),
                Instant.now(),
                payoutId,
                shopId,
                failureCode
        );
    }

    @Override
    public String aggregateId() {
        return payoutId.value().toString();
    }

    @Override
    public String eventType() {
        return "payout.failed";
    }
}