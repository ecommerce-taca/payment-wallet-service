package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentRefundedEvent(
        UUID eventId,
        Instant occurredAt,
        PaymentId paymentId,
        Money refundAmount,
        PaymentStatus newStatus
) implements DomainEvent {

    public PaymentRefundedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(refundAmount, "refundAmount must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
    }

    public static PaymentRefundedEvent now(
            PaymentId paymentId,
            Money refundAmount,
            PaymentStatus newStatus
    ) {
        return new PaymentRefundedEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId,
                refundAmount,
                newStatus
        );
    }

    @Override
    public String aggregateId() {
        return paymentId.value().toString();
    }

    @Override
    public String eventType() {
        return "payment.refunded";
    }
}
