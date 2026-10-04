package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.in.CleanupOutboxUseCase;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.OutboxCleanupPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class CleanupOutboxService
        implements CleanupOutboxUseCase {

    private final OutboxCleanupPort cleanupPort;
    private final ClockPort clockPort;
    private final TransactionPort transactionPort;
    private final Duration retention;
    private final int batchSize;

    public CleanupOutboxService(
            OutboxCleanupPort cleanupPort,
            ClockPort clockPort,
            TransactionPort transactionPort,
            Duration retention,
            int batchSize
    ) {
        this.cleanupPort = Objects.requireNonNull(cleanupPort);

        this.clockPort = Objects.requireNonNull(clockPort);

        this.transactionPort = Objects.requireNonNull(transactionPort);

        this.retention = Objects.requireNonNull(retention);

        if (retention.isZero()
                || retention.isNegative()) {
            throw new IllegalArgumentException(
                    "retention must be positive"
            );
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be positive"
            );
        }

        this.batchSize = batchSize;
    }

    @Override
    public int cleanup() {
        Instant cutoff = clockPort.now().minus(retention);

        return transactionPort.execute(
                () -> cleanupPort.deleteCompletedBefore(
                        cutoff,
                        batchSize
                )
        );
    }
}