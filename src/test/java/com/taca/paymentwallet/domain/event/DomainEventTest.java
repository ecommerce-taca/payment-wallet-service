package com.taca.paymentwallet.domain.event;

import com.taca.paymentwallet.domain.AggregateRoot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventTest {

    @Test
    void shouldRegisterAndClearDomainEvents() {
        TestAggregate aggregate = new TestAggregate();

        TestDomainEvent event = new TestDomainEvent(
                UUID.randomUUID(),
                Instant.now(),
                "aggregate-1",
                "test.created"
        );

        aggregate.doSomething(event);

        assertThat(aggregate.domainEvents()).hasSize(1);
        assertThat(aggregate.domainEvents().getFirst()).isEqualTo(event);

        aggregate.clearDomainEvents();

        assertThat(aggregate.domainEvents()).isEmpty();
    }

    private static final class TestAggregate extends AggregateRoot {

        void doSomething(DomainEvent event) {
            registerEvent(event);
        }
    }

    private record TestDomainEvent(
            UUID eventId,
            Instant occurredAt,
            String aggregateId,
            String eventType
    ) implements DomainEvent {
    }
}
