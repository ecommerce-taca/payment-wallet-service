package com.taca.paymentwallet.domain.refund;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;

import java.time.Instant;
import java.util.UUID;

public record RefundSucceededEvent(
        UUID eventId,
        Instant occurredAt,
        RefundId refundId,
        PaymentId paymentId,
        Money amount
) implements DomainEvent {

    public RefundSucceededEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        if (refundId == null) {
            throw new IllegalArgumentException("Refund id must not be null");
        }

        if (paymentId == null) {
            throw new IllegalArgumentException("Payment id must not be null");
        }
        
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
    }

    public static RefundSucceededEvent now(RefundId refundId, PaymentId paymentId, Money amount) {
        return new RefundSucceededEvent(
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
        return "refund.succeeded";
    }
}
