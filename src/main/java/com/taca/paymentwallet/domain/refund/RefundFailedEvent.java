package com.taca.paymentwallet.domain.refund;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RefundFailedEvent(
        UUID eventId,
        Instant occurredAt,
        RefundId refundId,
        PaymentId paymentId,
        String failureCode
) implements DomainEvent {

    public RefundFailedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(refundId, "refundId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");

        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }

        failureCode = failureCode.trim();
    }

    public static RefundFailedEvent now(
            RefundId refundId,
            PaymentId paymentId,
            String failureCode
    ) {
        return new RefundFailedEvent(
                UUID.randomUUID(),
                Instant.now(),
                refundId,
                paymentId,
                failureCode
        );
    }

    @Override
    public String aggregateId() {
        return refundId.value().toString();
    }

    @Override
    public String eventType() {
        return "refund.failed";
    }
}
