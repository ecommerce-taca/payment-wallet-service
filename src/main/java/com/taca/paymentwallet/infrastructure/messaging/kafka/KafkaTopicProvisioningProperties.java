package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.kafka.provisioning")
public record KafkaTopicProvisioningProperties(
        boolean enabled,
        Duration timeout
) {

    public KafkaTopicProvisioningProperties {
        if (timeout == null
                || timeout.isZero()
                || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must be positive"
            );
        }
    }
}