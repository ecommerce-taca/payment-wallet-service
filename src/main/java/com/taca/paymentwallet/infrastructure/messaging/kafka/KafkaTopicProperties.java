package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        String paymentEvents,
        String walletEvents,
        String shipmentEvents,
        String outboxDlq
) {

    public KafkaTopicProperties {
        requireText(paymentEvents, "paymentEvents");
        requireText(walletEvents, "walletEvents");
        requireText(shipmentEvents, "shipmentEvents");
        requireText(outboxDlq, "outboxDlq");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}