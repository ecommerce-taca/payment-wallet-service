package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.outbox.OutboxMessage;

import java.util.Set;

public interface OutboxMessagePublisherPort {

    Set<String> supportedEventTypes();

    default boolean supports(OutboxMessage message) {
        return supportedEventTypes().contains(message.eventType());
    }

    void publish(OutboxMessage message);
}