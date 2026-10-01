package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import com.taca.paymentwallet.application.port.in.PublishOutboxDeadLetterUseCase;
import com.taca.paymentwallet.application.port.out.*;

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

            deadLetterPort.markDeadLettered(
                    deadLetter.eventId(),
                    clockPort.now()
            );

            published++;
        }

        return published;
    }
}