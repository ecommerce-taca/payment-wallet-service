package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        String paymentEvents,
        String walletEvents
) {

    public KafkaTopicProperties {
        if (paymentEvents == null || paymentEvents.isBlank()) {
            throw new IllegalArgumentException("paymentEvents must not be blank");
        }

        if (walletEvents == null || walletEvents.isBlank()) {
            throw new IllegalArgumentException("walletEvents must not be blank");
        }
    }
}