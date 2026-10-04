package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.outbox.cleanup")
public record OutboxCleanupProperties(
        boolean enabled,
        Duration retention,
        int batchSize,
        long intervalMs
) {

    public OutboxCleanupProperties {
        if (retention == null
                || retention.isZero()
                || retention.isNegative()) {
            throw new IllegalArgumentException(
                    "retention must be positive"
            );
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be positive"
            );
        }

        if (intervalMs <= 0) {
            throw new IllegalArgumentException(
                    "intervalMs must be positive"
            );
        }
    }
}