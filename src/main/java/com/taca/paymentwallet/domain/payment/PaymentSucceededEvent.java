package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID eventId,
        Instant occurredAt,
        PaymentId paymentId,
        Money capturedAmount
) implements DomainEvent {

    public PaymentSucceededEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        if (paymentId == null) {
            throw new IllegalArgumentException("Payment id must not be null");
        }
        
        if (capturedAmount == null) {
            throw new IllegalArgumentException("Captured amount must not be null");
        }
    }

    public static PaymentSucceededEvent now(PaymentId paymentId, Money capturedAmount) {
        return new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId,
                capturedAmount
        );
    }

    @Override
    public String aggregateId() {
        return paymentId.value().toString();
    }

    @Override
    public String eventType() {
        return "payment.succeeded";
    }
}
