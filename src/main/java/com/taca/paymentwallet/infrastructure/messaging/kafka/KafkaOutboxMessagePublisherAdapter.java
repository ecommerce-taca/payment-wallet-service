package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.application.port.out.OutboxMessagePublisherPort;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Objects;

public class KafkaOutboxMessagePublisherAdapter implements OutboxMessagePublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicRouter topicRouter;

    public KafkaOutboxMessagePublisherAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicRouter topicRouter
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.topicRouter = Objects.requireNonNull(topicRouter);
    }

    @Override
    public boolean supports(OutboxMessage message) {
        return topicRouter.route(message).isPresent();
    }

    @Override
    public void publish(OutboxMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        String topic = topicRouter.route(message)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No Kafka topic configured for event type: " + message.eventType()
                ));

        String key = message.aggregateId().toString();

        try {
            kafkaTemplate.send(topic, key, message.payload()).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaOutboxPublishException(message.eventId(), exception);
        } catch (Exception exception) {
            throw new KafkaOutboxPublishException(message.eventId(), exception);
        }
    }
}