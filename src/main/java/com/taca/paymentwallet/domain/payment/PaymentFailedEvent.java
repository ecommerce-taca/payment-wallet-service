package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        Instant occurredAt,
        PaymentId paymentId,
        String failureCode
) implements DomainEvent {

    public PaymentFailedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        if (paymentId == null) {
            throw new IllegalArgumentException("Payment id must not be null");
        }
        
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("Failure code must not be blank");
        }
    }

    public static PaymentFailedEvent now(PaymentId paymentId, String failureCode) {
        return new PaymentFailedEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId,
                failureCode
        );
    }

    @Override
    public String aggregateId() {
        return paymentId.value().toString();
    }

    @Override
    public String eventType() {
        return "payment.failed";
    }
}
