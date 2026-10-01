package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.OutboxMessagePublisherPort;
import com.taca.paymentwallet.application.port.out.OutboxPublishingPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class PublishOutboxServiceTest {

    @Test
    void shouldPublishAndMarkEventAsPublished() {
        OutboxMessage message = message();

        FakeOutboxPublishingPort outboxPort =
                new FakeOutboxPublishingPort(List.of(message));

        FakePublisherPort publisherPort =
                new FakePublisherPort(false);

        SequenceClockPort clockPort = new SequenceClockPort(
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-01T00:00:01Z")
        );

        FakeTransactionPort transactionPort =
                new FakeTransactionPort();

        PublishOutboxService service = new PublishOutboxService(
                outboxPort,
                publisherPort,
                transactionPort,
                clockPort,
                50,
                3,
                Duration.ofSeconds(2)
        );

        int published = service.publishNextBatch();

        assertThat(published).isEqualTo(1);
        assertThat(publisherPort.publishedMessages)
                .containsExactly(message);

        assertThat(outboxPort.publishedEventId)
                .isEqualTo(message.eventId());

        assertThat(outboxPort.publishedAt)
                .isEqualTo(Instant.parse("2026-10-01T00:00:01Z"));

        assertThat(transactionPort.executed).isTrue();
    }

    @Test
    void shouldRecordFailureAndScheduleRetryAfterTwoSeconds() {
        OutboxMessage message = message();

        FakeOutboxPublishingPort outboxPort =
                new FakeOutboxPublishingPort(List.of(message));

        FakePublisherPort publisherPort =
                new FakePublisherPort(true);

        SequenceClockPort clockPort = new SequenceClockPort(
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-01T00:00:00Z")
        );

        PublishOutboxService service = new PublishOutboxService(
                outboxPort,
                publisherPort,
                new FakeTransactionPort(),
                clockPort,
                50,
                3,
                Duration.ofSeconds(2)
        );

        int published = service.publishNextBatch();

        assertThat(published).isZero();

        assertThat(outboxPort.failedEventId)
                .isEqualTo(message.eventId());

        assertThat(outboxPort.failureMessage)
                .isEqualTo("Kafka unavailable");

        assertThat(outboxPort.nextAttemptAt)
                .isEqualTo(Instant.parse("2026-10-01T00:00:02Z"));

        assertThat(outboxPort.publishedEventId).isNull();
    }

    @Test
    void shouldReturnZeroWhenBatchIsEmpty() {
        FakeOutboxPublishingPort outboxPort =
                new FakeOutboxPublishingPort(List.of());

        FakePublisherPort publisherPort =
                new FakePublisherPort(false);

        PublishOutboxService service = new PublishOutboxService(
                outboxPort,
                publisherPort,
                new FakeTransactionPort(),
                new SequenceClockPort(
                        Instant.parse("2026-10-01T00:00:00Z")
                ),
                50,
                3,
                Duration.ofSeconds(2)
        );

        int published = service.publishNextBatch();

        assertThat(published).isZero();
        assertThat(publisherPort.publishedMessages).isEmpty();
    }

    @Test
    void shouldPassPublishingConfigurationToRepository() {
        FakeOutboxPublishingPort outboxPort =
                new FakeOutboxPublishingPort(List.of());

        FakePublisherPort publisherPort =
                new FakePublisherPort(false);

        Instant now =
                Instant.parse("2026-10-01T00:00:00Z");

        PublishOutboxService service = new PublishOutboxService(
                outboxPort,
                publisherPort,
                new FakeTransactionPort(),
                new SequenceClockPort(now),
                25,
                3,
                Duration.ofSeconds(2)
        );

        service.publishNextBatch();

        assertThat(outboxPort.batchSize).isEqualTo(25);
        assertThat(outboxPort.maxRetries).isEqualTo(3);
        assertThat(outboxPort.lockTime).isEqualTo(now);
        assertThat(outboxPort.eventTypes)
                .containsExactlyInAnyOrder(
                        "payment.created",
                        "wallet.allocated"
                );
    }

    private OutboxMessage message() {
        return new OutboxMessage(
                UUID.randomUUID(),
                "PAYMENT",
                UUID.randomUUID(),
                "payment.created",
                "{\"status\":\"PENDING\"}",
                null,
                Instant.parse("2026-09-30T23:59:00Z"),
                0
        );
    }

    private static final class FakeOutboxPublishingPort
            implements OutboxPublishingPort {

        private final List<OutboxMessage> messages;

        private int batchSize;
        private int maxRetries;
        private Instant lockTime;
        private Set<String> eventTypes;

        private UUID publishedEventId;
        private Instant publishedAt;

        private UUID failedEventId;
        private String failureMessage;
        private Instant nextAttemptAt;

        private FakeOutboxPublishingPort(
                List<OutboxMessage> messages
        ) {
            this.messages = messages;
        }

        @Override
        public List<OutboxMessage> lockNextBatch(
                int batchSize,
                int maxRetries,
                Instant now,
                Set<String> eventTypes
        ) {
            this.batchSize = batchSize;
            this.maxRetries = maxRetries;
            this.lockTime = now;
            this.eventTypes = eventTypes;
            return messages;
        }

        @Override
        public void markPublished(
                UUID eventId,
                Instant publishedAt
        ) {
            this.publishedEventId = eventId;
            this.publishedAt = publishedAt;
        }

        @Override
        public void recordFailure(
                UUID eventId,
                String error,
                Instant nextAttemptAt
        ) {
            this.failedEventId = eventId;
            this.failureMessage = error;
            this.nextAttemptAt = nextAttemptAt;
        }
    }

    private static final class FakePublisherPort
            implements OutboxMessagePublisherPort {

        private final boolean shouldFail;
        private final List<OutboxMessage> publishedMessages =
                new ArrayList<>();

        private FakePublisherPort(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public Set<String> supportedEventTypes() {
            return Set.of(
                    "payment.created",
                    "wallet.allocated"
            );
        }

        @Override
        public void publish(OutboxMessage message) {
            if (shouldFail) {
                throw new RuntimeException("Kafka unavailable");
            }

            publishedMessages.add(message);
        }
    }

    private static final class SequenceClockPort
            implements ClockPort {

        private final List<Instant> values;
        private int index;

        private SequenceClockPort(Instant... values) {
            this.values = List.of(values);
        }

        @Override
        public Instant now() {
            if (index >= values.size()) {
                return values.getLast();
            }

            return values.get(index++);
        }
    }

    private static final class FakeTransactionPort
            implements TransactionPort {

        private boolean executed;

        @Override
        public <T> T execute(Supplier<T> action) {
            executed = true;
            return action.get();
        }
    }
}