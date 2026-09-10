package com.taca.paymentwallet.domain.settlement;

import com.taca.paymentwallet.domain.event.DomainEvent;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;

import java.time.Instant;
import java.util.UUID;

public record SettlementBatchCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        SettlementBatchId settlementBatchId,
        Money totalReleased
) implements DomainEvent {

    public SettlementBatchCompletedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("Event id must not be null");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        if (settlementBatchId == null) {
            throw new IllegalArgumentException("Settlement batch id must not be null");
        }

        if (totalReleased == null) {
            throw new IllegalArgumentException("Total released must not be null");
        }
    }

    public static SettlementBatchCompletedEvent now(
            SettlementBatchId settlementBatchId,
            Money totalReleased
    ) {
        return new SettlementBatchCompletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                settlementBatchId,
                totalReleased
        );
    }

    @Override
    public String aggregateId() {
        return settlementBatchId.value().toString();
    }

    @Override
    public String eventType() {
        return "settlement.batch.completed";
    }
}
