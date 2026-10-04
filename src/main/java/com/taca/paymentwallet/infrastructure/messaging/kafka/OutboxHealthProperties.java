package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.outbox.health")
public record OutboxHealthProperties(
        boolean enabled,
        Duration maxLag
) {

    public OutboxHealthProperties {
        if (maxLag == null
                || maxLag.isZero()
                || maxLag.isNegative()) {
            throw new IllegalArgumentException(
                    "maxLag must be positive"
            );
        }
    }
}