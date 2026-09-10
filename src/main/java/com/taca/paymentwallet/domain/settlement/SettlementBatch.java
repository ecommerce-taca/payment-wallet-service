package com.taca.paymentwallet.domain.settlement;

import com.taca.paymentwallet.domain.AggregateRoot;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettlementBatch extends AggregateRoot {

    private final SettlementBatchId id;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final List<SettlementBatchItem> items;
    private SettlementBatchStatus status;

    public SettlementBatch(
            SettlementBatchId id,
            Instant periodStart,
            Instant periodEnd,
            List<SettlementBatchItem> items
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (periodStart == null) {
            throw new IllegalArgumentException("periodStart must not be null");
        }

        if (periodEnd == null) {
            throw new IllegalArgumentException("periodEnd must not be null");
        }

        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException("periodStart must be before periodEnd");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }

        this.id = id;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.items = List.copyOf(items);
        this.status = SettlementBatchStatus.PENDING;

        validateNoDuplicatePaymentAllocation();
    }

    public void markProcessing() {
        if (status != SettlementBatchStatus.PENDING) {
            throw new InvalidSettlementStateException("Only pending batch can be processing");
        }

        this.status = SettlementBatchStatus.PROCESSING;
    }

    public void markCompleted() {
        if (status != SettlementBatchStatus.PROCESSING) {
            throw new InvalidSettlementStateException("Only processing batch can be completed");
        }

        boolean allCompleted = items.stream()
                .allMatch(item -> item.status() == SettlementBatchItemStatus.COMPLETED);

        if (!allCompleted) {
            throw new InvalidSettlementStateException("All batch items must be completed");
        }

        this.status = SettlementBatchStatus.COMPLETED;

        registerEvent(SettlementBatchCompletedEvent.now(id, totalReleased()));
    }

    public void markFailed() {
        if (status == SettlementBatchStatus.COMPLETED) {
            throw new InvalidSettlementStateException("Completed batch cannot be failed");
        }

        this.status = SettlementBatchStatus.FAILED;
    }

    public Money totalReleased() {
        return items.stream()
                .map(SettlementBatchItem::releasedAmount)
                .reduce(Money.vnd(0), Money::add);
    }

    private void validateNoDuplicatePaymentAllocation() {
        Set<PaymentAllocationId> ids = new HashSet<>();

        for (SettlementBatchItem item : items) {
            for (SettlementLine line : item.lines()) {
                if (!ids.add(line.paymentAllocationId())) {
                    throw new IllegalArgumentException("payment allocation can be settled only once");
                }
            }
        }
    }

    public SettlementBatchId id() {
        return id;
    }

    public Instant periodStart() {
        return periodStart;
    }

    public Instant periodEnd() {
        return periodEnd;
    }

    public List<SettlementBatchItem> items() {
        return items;
    }

    public SettlementBatchStatus status() {
        return status;
    }
}
