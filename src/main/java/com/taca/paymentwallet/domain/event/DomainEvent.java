package com.taca.paymentwallet.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String aggregateId();

    String eventType();
}
