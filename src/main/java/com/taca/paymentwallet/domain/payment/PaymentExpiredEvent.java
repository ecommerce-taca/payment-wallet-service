package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentExpiredEvent(
        UUID eventId,
        Instant occurredAt,
        PaymentId paymentId
) implements DomainEvent {

    public PaymentExpiredEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
    }

    public static PaymentExpiredEvent now(PaymentId paymentId) {
        return new PaymentExpiredEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId
        );
    }

    @Override
    public String aggregateId() {
        return paymentId.value().toString();
    }

    @Override
    public String eventType() {
        return "payment.expired";
    }
}
