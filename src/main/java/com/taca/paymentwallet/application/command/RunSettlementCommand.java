package com.taca.paymentwallet.application.command;

import java.time.Instant;

public record RunSettlementCommand(
        Instant periodStart,
        Instant periodEnd
) {

    public RunSettlementCommand {
        if (periodStart == null) {
            throw new IllegalArgumentException("periodStart must not be null");
        }

        if (periodEnd == null) {
            throw new IllegalArgumentException("periodEnd must not be null");
        }

        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException("periodStart must be before periodEnd");
        }
    }
}