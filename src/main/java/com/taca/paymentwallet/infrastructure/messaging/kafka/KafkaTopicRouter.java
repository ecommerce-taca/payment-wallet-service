package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;

import java.util.Objects;
import java.util.Optional;

public class KafkaTopicRouter {

    private final KafkaTopicProperties properties;

    public KafkaTopicRouter(KafkaTopicProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public Optional<String> route(OutboxMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        return switch (message.eventType()) {
            case "payment.created" -> Optional.of(properties.paymentEvents());
            case "wallet.allocated" -> Optional.of(properties.walletEvents());
            default -> Optional.empty();
        };
    }
}