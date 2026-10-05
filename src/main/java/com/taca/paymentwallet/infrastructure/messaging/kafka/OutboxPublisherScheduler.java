package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.port.in.PublishOutboxDeadLetterUseCase;
import com.taca.paymentwallet.application.port.in.PublishOutboxUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "app.outbox.publisher",
        name = "enabled",
        havingValue = "true"
)
public class OutboxPublisherScheduler {

    private final PublishOutboxUseCase publishOutboxUseCase;

    private final PublishOutboxDeadLetterUseCase publishDeadLetterUseCase;

    public OutboxPublisherScheduler(
            PublishOutboxUseCase publishOutboxUseCase,
            PublishOutboxDeadLetterUseCase publishDeadLetterUseCase
    ) {
        this.publishOutboxUseCase = Objects.requireNonNull(publishOutboxUseCase);
        this.publishDeadLetterUseCase = Objects.requireNonNull(publishDeadLetterUseCase);
    }

    @Scheduled(
            fixedDelayString = "${app.outbox.publisher.poll-interval-ms:1000}"
    )
    public void publish() {
        publishOutboxUseCase.publishNextBatch();
        publishDeadLetterUseCase.publishNextBatch();
    }
}