package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import com.taca.paymentwallet.application.port.in.PublishOutboxDeadLetterUseCase;
import com.taca.paymentwallet.application.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class PublishOutboxDeadLetterService
        implements PublishOutboxDeadLetterUseCase {

    private final OutboxDeadLetterPort deadLetterPort;
    private final DeadLetterPublisherPort publisherPort;
    private final OutboxMessagePublisherPort messagePublisherPort;
    private final TransactionPort transactionPort;
    private final ClockPort clockPort;
    private final int batchSize;
    private final int maxRetries;

    private static final Logger log = LoggerFactory.getLogger(PublishOutboxDeadLetterService.class);

    public PublishOutboxDeadLetterService(
            OutboxDeadLetterPort deadLetterPort,
            DeadLetterPublisherPort publisherPort,
            OutboxMessagePublisherPort messagePublisherPort,
            TransactionPort transactionPort,
            ClockPort clockPort,
            int batchSize,
            int maxRetries
    ) {
        this.deadLetterPort = Objects.requireNonNull(deadLetterPort);
        this.publisherPort = Objects.requireNonNull(publisherPort);
        this.messagePublisherPort = Objects.requireNonNull(messagePublisherPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.clockPort = Objects.requireNonNull(clockPort);

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }

        if (maxRetries <= 0) {
            throw new IllegalArgumentException("maxRetries must be positive");
        }

        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Override
    public int publishNextBatch() {
        return transactionPort.execute(this::publishInsideTransaction);
    }

    private int publishInsideTransaction() {
        List<OutboxDeadLetter> deadLetters =
                deadLetterPort.lockNextDeadLetterBatch(
                        batchSize,
                        maxRetries,
                        messagePublisherPort.supportedEventTypes()
                );

        int published = 0;

        for (OutboxDeadLetter deadLetter : deadLetters) {
            publisherPort.publish(deadLetter);

            var deadLetteredAt = clockPort.now();

            deadLetterPort.markDeadLettered(
                    deadLetter.eventId(),
                    deadLetteredAt
            );

            log.warn(
                    "event=outbox_mark_dead_lettered event_id={} event_type={} retry_count={} dead_lettered_at={}",
                    deadLetter.eventId(),
                    deadLetter.eventType(),
                    deadLetter.retryCount(),
                    deadLetteredAt
            );

            published++;
        }

        return published;
    }
}