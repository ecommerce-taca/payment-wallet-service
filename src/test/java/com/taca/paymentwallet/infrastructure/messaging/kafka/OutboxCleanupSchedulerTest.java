package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.port.in.CleanupOutboxUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OutboxCleanupSchedulerTest {

    @Test
    void shouldRunCleanupUseCase() {
        CleanupOutboxUseCase useCase =
                mock(CleanupOutboxUseCase.class);

        when(useCase.cleanup())
                .thenReturn(10);

        OutboxCleanupScheduler scheduler =
                new OutboxCleanupScheduler(
                        useCase
                );

        scheduler.cleanup();

        verify(useCase)
                .cleanup();
    }
}