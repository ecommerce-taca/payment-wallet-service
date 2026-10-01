package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.application.port.out.OutboxMessagePublisherPort;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Objects;
import java.util.Set;

public class KafkaOutboxMessagePublisherAdapter implements OutboxMessagePublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicRouter topicRouter;
    private final KafkaHeaderMapper headerMapper;
    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            "payment.created",
            "wallet.allocated"
    );

    public KafkaOutboxMessagePublisherAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicRouter topicRouter,
            KafkaHeaderMapper headerMapper
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.topicRouter = Objects.requireNonNull(topicRouter);
        this.headerMapper = Objects.requireNonNull(headerMapper);
    }

    @Override
    public boolean supports(OutboxMessage message) {
        return topicRouter.route(message).isPresent();
    }

    @Override
    public Set<String> supportedEventTypes() {
        return SUPPORTED_EVENT_TYPES;
    }

    @Override
    public void publish(OutboxMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        String topic = topicRouter.route(message)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No Kafka topic configured for event type: " + message.eventType()
                ));

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        topic,
                        message.aggregateId().toString(),
                        message.payload()
                );

        headerMapper.apply(record, message);

        try {
            kafkaTemplate.send(record).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new KafkaOutboxPublishException(
                    message.eventId(),
                    exception
            );
        } catch (Exception exception) {
            throw new KafkaOutboxPublishException(
                    message.eventId(),
                    exception
            );
        }
    }
}