package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.port.in.CleanupOutboxUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "app.outbox.cleanup",
        name = "enabled",
        havingValue = "true"
)
public class OutboxCleanupScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OutboxCleanupScheduler.class
            );

    private final CleanupOutboxUseCase cleanupOutboxUseCase;

    public OutboxCleanupScheduler(
            CleanupOutboxUseCase cleanupOutboxUseCase
    ) {
        this.cleanupOutboxUseCase =
                Objects.requireNonNull(
                        cleanupOutboxUseCase
                );
    }

    @Scheduled(
            fixedDelayString =
                    "${app.outbox.cleanup.interval-ms:3600000}"
    )
    public void cleanup() {
        int deleted =
                cleanupOutboxUseCase.cleanup();

        if (deleted > 0) {
            log.info(
                    "event=outbox_cleanup deleted_count={}",
                    deleted
            );
        }
    }
}