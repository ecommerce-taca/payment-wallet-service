package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.application.port.in.PublishOutboxUseCase;
import com.taca.paymentwallet.application.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class PublishOutboxService implements PublishOutboxUseCase {

    private final OutboxPublishingPort outboxPublishingPort;
    private final OutboxMessagePublisherPort publisherPort;
    private final TransactionPort transactionPort;
    private final ClockPort clockPort;
    private final int batchSize;
    private final int maxRetries;
    private final Duration retryBackoff;
    private static final Logger log = LoggerFactory.getLogger(PublishOutboxService.class);

    public PublishOutboxService(
            OutboxPublishingPort outboxPublishingPort,
            OutboxMessagePublisherPort publisherPort,
            TransactionPort transactionPort,
            ClockPort clockPort,
            int batchSize,
            int maxRetries,
            Duration retryBackoff
    ) {
        this.outboxPublishingPort = Objects.requireNonNull(outboxPublishingPort);
        this.publisherPort = Objects.requireNonNull(publisherPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.retryBackoff = Objects.requireNonNull(retryBackoff);

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }

        if (maxRetries <= 0) {
            throw new IllegalArgumentException("maxRetries must be positive");
        }

        if (retryBackoff.isNegative() || retryBackoff.isZero()) {
            throw new IllegalArgumentException("retryBackoff must be positive");
        }

        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Override
    public int publishNextBatch() {
        return transactionPort.execute(this::publishInsideTransaction);
    }

    private int publishInsideTransaction() {
        Instant now = clockPort.now();

        List<OutboxMessage> messages = outboxPublishingPort.lockNextBatch(
                batchSize,
                maxRetries,
                now,
                publisherPort.supportedEventTypes()
        );

        int published = 0;

        for (OutboxMessage message : messages) {
            if (publish(message)) {
                published++;
            }
        }

        return published;
    }

    private boolean publish(OutboxMessage message) {
        try {
            publisherPort.publish(message);

            Instant publishedAt = clockPort.now();

            outboxPublishingPort.markPublished(
                    message.eventId(),
                    publishedAt
            );

            log.debug(
                    "event=outbox_mark_published event_id={} event_type={} published_at={}",
                    message.eventId(),
                    message.eventType(),
                    publishedAt
            );

            return true;
        } catch (RuntimeException exception) {
            Instant nextAttemptAt =
                    clockPort.now().plus(retryBackoff);

            String error =
                    errorMessage(exception);

            outboxPublishingPort.recordFailure(
                    message.eventId(),
                    error,
                    nextAttemptAt
            );

            log.warn(
                    "event=outbox_retry_scheduled event_id={} event_type={} retry_count={} next_attempt_at={} error_type={}",
                    message.eventId(),
                    message.eventType(),
                    message.retryCount() + 1,
                    nextAttemptAt,
                    exception.getClass().getSimpleName()
            );

            return false;
        }
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}