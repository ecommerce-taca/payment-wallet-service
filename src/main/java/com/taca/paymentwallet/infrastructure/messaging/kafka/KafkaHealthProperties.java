package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.kafka.health")
public record KafkaHealthProperties(
        boolean enabled,
        Duration timeout
) {

    public KafkaHealthProperties {
        if (timeout == null
                || timeout.isZero()
                || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must be positive"
            );
        }
    }
}