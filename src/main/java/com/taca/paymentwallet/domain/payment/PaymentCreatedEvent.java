package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.OrderId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PaymentCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        PaymentId paymentId,
        CheckoutGroupId checkoutGroupId,
        List<OrderId> orderIds,
        Money amount,
        PaymentMethod method,
        PaymentStatus status
) implements DomainEvent {

    public PaymentCreatedEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(checkoutGroupId);
        Objects.requireNonNull(orderIds);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(method);
        Objects.requireNonNull(status);

        if (orderIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "orderIds must not be empty"
            );
        }

        orderIds = List.copyOf(orderIds);
    }

    @Override
    public String aggregateId() {
        return paymentId.value().toString();
    }

    @Override
    public String eventType() {
        return "payment.created";
    }
}