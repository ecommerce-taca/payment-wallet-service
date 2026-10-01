package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import com.taca.paymentwallet.application.port.out.DeadLetterPublisherPort;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public class KafkaDeadLetterPublisherAdapter implements DeadLetterPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicProperties properties;
    private final ObjectMapper objectMapper;
    private final KafkaDeadLetterHeaderMapper headerMapper;

    public KafkaDeadLetterPublisherAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicProperties properties,
            ObjectMapper objectMapper,
            KafkaDeadLetterHeaderMapper headerMapper
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.headerMapper = Objects.requireNonNull(headerMapper);
    }

    @Override
    public void publish(OutboxDeadLetter deadLetter) {
        Objects.requireNonNull(deadLetter, "deadLetter must not be null");

        KafkaDeadLetterPayload payload = new KafkaDeadLetterPayload(
                deadLetter.eventId(),
                deadLetter.aggregateType(),
                deadLetter.aggregateId(),
                deadLetter.eventType(),
                deadLetter.payload(),
                deadLetter.headers(),
                deadLetter.occurredAt(),
                deadLetter.retryCount(),
                deadLetter.lastError()
        );

        try {
            String json = objectMapper.writeValueAsString(payload);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(
                            properties.outboxDlq(),
                            deadLetter.aggregateId().toString(),
                            json
                    );

            headerMapper.apply(record, deadLetter);

            kafkaTemplate.send(record).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaOutboxPublishException(
                    deadLetter.eventId(),
                    exception
            );
        } catch (Exception exception) {
            throw new KafkaOutboxPublishException(
                    deadLetter.eventId(),
                    exception
            );
        }
    }
}