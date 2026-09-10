package com.taca.paymentwallet.domain.refund;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RefundRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        RefundId refundId,
        PaymentId paymentId,
        Money amount
) implements DomainEvent {

    public RefundRequestedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(refundId, "refundId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }

    public static RefundRequestedEvent now(
            RefundId refundId,
            PaymentId paymentId,
            Money amount
    ) {
        return new RefundRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                refundId,
                paymentId,
                amount
        );
    }

    @Override
    public String aggregateId() {
        return refundId.value().toString();
    }

    @Override
    public String eventType() {
        return "refund.requested";
    }
}
