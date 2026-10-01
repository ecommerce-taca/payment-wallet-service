package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.application.port.out.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublishOutboxDeadLetterServiceTest {

    @Test
    void shouldPublishDeadLetterAndMarkEvent() {
        OutboxDeadLetter deadLetter = deadLetter();

        FakeDeadLetterPort deadLetterPort =
                new FakeDeadLetterPort(List.of(deadLetter));

        FakeDeadLetterPublisherPort publisherPort =
                new FakeDeadLetterPublisherPort(false);

        FixedClockPort clockPort = new FixedClockPort(
                Instant.parse("2026-10-01T10:05:00Z")
        );

        FakeTransactionPort transactionPort =
                new FakeTransactionPort();

        PublishOutboxDeadLetterService service =
                new PublishOutboxDeadLetterService(
                        deadLetterPort,
                        publisherPort,
                        new FakeMessagePublisherPort(),
                        transactionPort,
                        clockPort,
                        50,
                        3
                );

        int published = service.publishNextBatch();

        assertThat(published).isEqualTo(1);
        assertThat(publisherPort.published)
                .containsExactly(deadLetter);

        assertThat(deadLetterPort.markedEventId)
                .isEqualTo(deadLetter.eventId());

        assertThat(deadLetterPort.deadLetteredAt)
                .isEqualTo(Instant.parse("2026-10-01T10:05:00Z"));

        assertThat(transactionPort.executed).isTrue();
    }

    @Test
    void shouldNotMarkDeadLetteredWhenKafkaPublishFails() {
        OutboxDeadLetter deadLetter = deadLetter();

        FakeDeadLetterPort deadLetterPort =
                new FakeDeadLetterPort(List.of(deadLetter));

        FakeDeadLetterPublisherPort publisherPort =
                new FakeDeadLetterPublisherPort(true);

        PublishOutboxDeadLetterService service =
                new PublishOutboxDeadLetterService(
                        deadLetterPort,
                        publisherPort,
                        new FakeMessagePublisherPort(),
                        new FakeTransactionPort(),
                        new FixedClockPort(
                                Instant.parse("2026-10-01T10:05:00Z")
                        ),
                        50,
                        3
                );

        assertThatThrownBy(service::publishNextBatch)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DLQ unavailable");

        assertThat(deadLetterPort.markedEventId).isNull();
    }

    @Test
    void shouldReturnZeroWhenNoDeadLettersExist() {
        FakeDeadLetterPort deadLetterPort =
                new FakeDeadLetterPort(List.of());

        PublishOutboxDeadLetterService service =
                new PublishOutboxDeadLetterService(
                        deadLetterPort,
                        new FakeDeadLetterPublisherPort(false),
                        new FakeMessagePublisherPort(),
                        new FakeTransactionPort(),
                        new FixedClockPort(Instant.now()),
                        50,
                        3
                );

        assertThat(service.publishNextBatch()).isZero();
    }

    @Test
    void shouldPassConfigurationAndSupportedEventTypes() {
        FakeDeadLetterPort deadLetterPort =
                new FakeDeadLetterPort(List.of());

        PublishOutboxDeadLetterService service =
                new PublishOutboxDeadLetterService(
                        deadLetterPort,
                        new FakeDeadLetterPublisherPort(false),
                        new FakeMessagePublisherPort(),
                        new FakeTransactionPort(),
                        new FixedClockPort(Instant.now()),
                        25,
                        3
                );

        service.publishNextBatch();

        assertThat(deadLetterPort.batchSize).isEqualTo(25);
        assertThat(deadLetterPort.maxRetries).isEqualTo(3);
        assertThat(deadLetterPort.eventTypes)
                .containsExactlyInAnyOrder(
                        "payment.created",
                        "wallet.allocated"
                );
    }

    private OutboxDeadLetter deadLetter() {
        return new OutboxDeadLetter(
                UUID.randomUUID(),
                "PAYMENT",
                UUID.randomUUID(),
                "payment.created",
                "{}",
                null,
                Instant.parse("2026-10-01T10:00:00Z"),
                3,
                "Kafka unavailable"
        );
    }

    private static final class FakeDeadLetterPort
            implements OutboxDeadLetterPort {

        private final List<OutboxDeadLetter> deadLetters;

        private int batchSize;
        private int maxRetries;
        private Set<String> eventTypes;

        private UUID markedEventId;
        private Instant deadLetteredAt;

        private FakeDeadLetterPort(
                List<OutboxDeadLetter> deadLetters
        ) {
            this.deadLetters = deadLetters;
        }

        @Override
        public List<OutboxDeadLetter> lockNextDeadLetterBatch(
                int batchSize,
                int maxRetries,
                Set<String> eventTypes
        ) {
            this.batchSize = batchSize;
            this.maxRetries = maxRetries;
            this.eventTypes = eventTypes;
            return deadLetters;
        }

        @Override
        public void markDeadLettered(
                UUID eventId,
                Instant deadLetteredAt
        ) {
            this.markedEventId = eventId;
            this.deadLetteredAt = deadLetteredAt;
        }
    }

    private static final class FakeDeadLetterPublisherPort
            implements DeadLetterPublisherPort {

        private final boolean shouldFail;
        private final List<OutboxDeadLetter> published =
                new java.util.ArrayList<>();

        private FakeDeadLetterPublisherPort(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public void publish(OutboxDeadLetter deadLetter) {
            if (shouldFail) {
                throw new RuntimeException("DLQ unavailable");
            }

            published.add(deadLetter);
        }
    }

    private static final class FakeMessagePublisherPort
            implements OutboxMessagePublisherPort {

        @Override
        public Set<String> supportedEventTypes() {
            return Set.of(
                    "payment.created",
                    "wallet.allocated"
            );
        }

        @Override
        public void publish(OutboxMessage message) {
            throw new UnsupportedOperationException();
        }
    }

    private record FixedClockPort(Instant now)
            implements ClockPort {
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