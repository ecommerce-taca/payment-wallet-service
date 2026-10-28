package com.taca.paymentwallet.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class OutboxOperationalObservation {

    static final String PENDING_COUNT =
            "payment_wallet_outbox_pending";

    static final String OLDEST_LAG_SECONDS =
            "payment_wallet_outbox_oldest_lag_seconds";

    static final String CLEANUP_DELETED_TOTAL =
            "payment_wallet_outbox_cleanup_deleted_total";

    private final AtomicLong pendingCount =
            new AtomicLong();

    private final AtomicLong oldestLagSeconds =
            new AtomicLong();

    private final Counter cleanupDeletedCounter;

    public OutboxOperationalObservation(
            MeterRegistry meterRegistry
    ) {
        Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null"
        );

        Gauge.builder(
                        PENDING_COUNT,
                        pendingCount,
                        AtomicLong::doubleValue
                )
                .register(meterRegistry);

        Gauge.builder(
                        OLDEST_LAG_SECONDS,
                        oldestLagSeconds,
                        AtomicLong::doubleValue
                )
                .register(meterRegistry);

        cleanupDeletedCounter = Counter.builder(
                        CLEANUP_DELETED_TOTAL
                )
                .register(meterRegistry);
    }

    public void recordBacklog(
            long pending,
            Duration oldestLag
    ) {
        Objects.requireNonNull(
                oldestLag,
                "oldestLag must not be null"
        );

        if (pending < 0) {
            throw new IllegalArgumentException(
                    "pending must not be negative"
            );
        }

        if (oldestLag.isNegative()) {
            throw new IllegalArgumentException(
                    "oldestLag must not be negative"
            );
        }

        pendingCount.set(pending);
        oldestLagSeconds.set(
                oldestLag.toSeconds()
        );
    }

    public void recordCleanup(int deleted) {
        if (deleted < 0) {
            throw new IllegalArgumentException(
                    "deleted must not be negative"
            );
        }

        cleanupDeletedCounter.increment(deleted);
    }
}