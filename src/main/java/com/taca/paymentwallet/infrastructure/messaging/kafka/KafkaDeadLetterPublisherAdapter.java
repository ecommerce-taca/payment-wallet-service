package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxDeadLetter;
import com.taca.paymentwallet.application.port.out.DeadLetterPublisherPort;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class KafkaDeadLetterPublisherAdapter implements DeadLetterPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicProperties properties;
    private final ObjectMapper objectMapper;
    private final KafkaDeadLetterHeaderMapper headerMapper;
    private final OutboxPublisherObservation observation;
    private final long sendTimeoutMs;

    private static final Logger log = LoggerFactory.getLogger(KafkaDeadLetterPublisherAdapter.class);

    public KafkaDeadLetterPublisherAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicProperties properties,
            ObjectMapper objectMapper,
            KafkaDeadLetterHeaderMapper headerMapper,
            OutboxPublisherObservation observation,
            long sendTimeoutMs
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.headerMapper = Objects.requireNonNull(headerMapper);
        this.observation = Objects.requireNonNull(observation);

        if (sendTimeoutMs <= 0) {
            throw new IllegalArgumentException(
                    "sendTimeoutMs must be positive"
            );
        }

        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Override
    public void publish(OutboxDeadLetter deadLetter) {
        Objects.requireNonNull(
                deadLetter,
                "deadLetter must not be null"
        );

        KafkaDeadLetterPayload payload =
                new KafkaDeadLetterPayload(
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

        String topic = properties.outboxDlq();
        long startedAt = System.nanoTime();

        try {
            String json =
                    objectMapper.writeValueAsString(payload);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(
                            topic,
                            deadLetter.aggregateId().toString(),
                            json
                    );

            headerMapper.apply(record, deadLetter);

            kafkaTemplate.send(record).get(
                    sendTimeoutMs,
                    TimeUnit.MILLISECONDS
            );

            Duration duration = elapsed(startedAt);

            observation.recordDeadLetter(
                    deadLetter.eventType(),
                    topic,
                    "success",
                    duration
            );

            log.warn(
                    "event=outbox_dlq_publish_success event_id={} event_type={} topic={} retry_count={} duration_ms={}",
                    deadLetter.eventId(),
                    deadLetter.eventType(),
                    topic,
                    deadLetter.retryCount(),
                    duration.toMillis()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            recordFailure(
                    deadLetter,
                    topic,
                    startedAt,
                    exception
            );

            throw new KafkaOutboxPublishException(
                    deadLetter.eventId(),
                    exception
            );
        } catch (Exception exception) {
            recordFailure(
                    deadLetter,
                    topic,
                    startedAt,
                    exception
            );

            throw new KafkaOutboxPublishException(
                    deadLetter.eventId(),
                    exception
            );
        }
    }

    private void recordFailure(
            OutboxDeadLetter deadLetter,
            String topic,
            long startedAt,
            Exception exception
    ) {
        Duration duration = elapsed(startedAt);

        observation.recordDeadLetter(
                deadLetter.eventType(),
                topic,
                "failure",
                duration
        );

        log.error(
                "event=outbox_dlq_publish_failure event_id={} event_type={} topic={} retry_count={} duration_ms={} error_type={}",
                deadLetter.eventId(),
                deadLetter.eventType(),
                topic,
                deadLetter.retryCount(),
                duration.toMillis(),
                exception.getClass().getSimpleName()
        );
    }

    private Duration elapsed(long startedAt) {
        return Duration.ofNanos(
                System.nanoTime() - startedAt
        );
    }
}