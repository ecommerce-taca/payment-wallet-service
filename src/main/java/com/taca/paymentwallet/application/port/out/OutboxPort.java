package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.event.DomainEvent;

import java.util.List;
import java.util.Objects;

public interface OutboxPort {

    void save(DomainEvent event);

    default void saveAll(List<DomainEvent> events) {
        Objects.requireNonNull(events, "events must not be null")
                .forEach(this::save);
    }
}
