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

        OutboxOperationalObservation observation =
                mock(OutboxOperationalObservation.class);

        OutboxCleanupScheduler scheduler =
                new OutboxCleanupScheduler(
                        useCase,
                        observation
                );

        scheduler.cleanup();

        verify(useCase).cleanup();

        verify(observation).recordCleanup(10);
    }

    @Test
    void shouldRecordZeroCleanupResult() {
        CleanupOutboxUseCase useCase = mock(CleanupOutboxUseCase.class);

        OutboxOperationalObservation observation =
                mock(OutboxOperationalObservation.class);

        when(useCase.cleanup()).thenReturn(0);

        OutboxCleanupScheduler scheduler =
                new OutboxCleanupScheduler(
                        useCase,
                        observation
                );

        scheduler.cleanup();

        verify(observation).recordCleanup(0);
    }
}