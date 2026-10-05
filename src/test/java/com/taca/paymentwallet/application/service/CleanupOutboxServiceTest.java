package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.OutboxCleanupPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class CleanupOutboxServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-04T10:00:00Z");

    @Test
    void shouldDeleteCompletedEventsOlderThanRetention() {
        FakeCleanupPort cleanupPort =
                new FakeCleanupPort(12);

        FakeTransactionPort transactionPort =
                new FakeTransactionPort();

        CleanupOutboxService service =
                new CleanupOutboxService(
                        cleanupPort,
                        () -> NOW,
                        transactionPort,
                        Duration.ofDays(7),
                        500
                );

        int deleted = service.cleanup();

        assertThat(deleted)
                .isEqualTo(12);

        assertThat(cleanupPort.cutoff)
                .isEqualTo(
                        NOW.minus(Duration.ofDays(7))
                );

        assertThat(cleanupPort.batchSize)
                .isEqualTo(500);

        assertThat(transactionPort.executed)
                .isTrue();
    }

    private static final class FakeCleanupPort
            implements OutboxCleanupPort {

        private final int result;

        private Instant cutoff;
        private int batchSize;

        private FakeCleanupPort(int result) {
            this.result = result;
        }

        @Override
        public int deleteCompletedBefore(
                Instant cutoff,
                int batchSize
        ) {
            this.cutoff = cutoff;
            this.batchSize = batchSize;
            return result;
        }
    }

    private static final class FakeTransactionPort
            implements TransactionPort {

        private boolean executed;

        @Override
        public <T> T execute(
                Supplier<T> action
        ) {
            executed = true;
            return action.get();
        }
    }
}