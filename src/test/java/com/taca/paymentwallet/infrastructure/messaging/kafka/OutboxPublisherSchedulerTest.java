package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.port.in.PublishOutboxDeadLetterUseCase;
import com.taca.paymentwallet.application.port.in.PublishOutboxUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OutboxPublisherSchedulerTest {

    @Test
    void shouldRunNormalPublisherBeforeDeadLetterPublisher() {
        PublishOutboxUseCase publishOutboxUseCase =
                mock(PublishOutboxUseCase.class);

        PublishOutboxDeadLetterUseCase deadLetterUseCase =
                mock(PublishOutboxDeadLetterUseCase.class);

        OutboxPublisherScheduler scheduler =
                new OutboxPublisherScheduler(
                        publishOutboxUseCase,
                        deadLetterUseCase
                );

        scheduler.publish();

        var order = inOrder(
                publishOutboxUseCase,
                deadLetterUseCase
        );

        order.verify(publishOutboxUseCase)
                .publishNextBatch();

        order.verify(deadLetterUseCase)
                .publishNextBatch();
    }
}