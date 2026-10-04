package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.outbox.OutboxMessage;
import com.taca.paymentwallet.application.port.out.OutboxMessagePublisherPort;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

public class KafkaOutboxMessagePublisherAdapter
        implements OutboxMessagePublisherPort {

    private static final Logger log =
            LoggerFactory.getLogger(
                    KafkaOutboxMessagePublisherAdapter.class
            );

    private static final Set<String> SUPPORTED_EVENT_TYPES =
            Set.of(
                    "payment.created",
                    "wallet.allocated"
            );

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicRouter topicRouter;
    private final KafkaHeaderMapper headerMapper;
    private final OutboxPublisherObservation observation;

    public KafkaOutboxMessagePublisherAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicRouter topicRouter,
            KafkaHeaderMapper headerMapper,
            OutboxPublisherObservation observation
    ) {
        this.kafkaTemplate =
                Objects.requireNonNull(kafkaTemplate);

        this.topicRouter =
                Objects.requireNonNull(topicRouter);

        this.headerMapper =
                Objects.requireNonNull(headerMapper);

        this.observation =
                Objects.requireNonNull(observation);
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
        Objects.requireNonNull(
                message,
                "message must not be null"
        );

        String topic = topicRouter.route(message)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No Kafka topic configured for event type: "
                                        + message.eventType()
                        )
                );

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        topic,
                        message.aggregateId().toString(),
                        message.payload()
                );

        headerMapper.apply(record, message);

        long startedAt = System.nanoTime();

        try {
            kafkaTemplate.send(record).get();

            Duration duration =
                    elapsed(startedAt);

            observation.recordPublish(
                    message.eventType(),
                    topic,
                    "success",
                    duration
            );

            log.info(
                    "event=outbox_publish_success event_id={} event_type={} topic={} duration_ms={}",
                    message.eventId(),
                    message.eventType(),
                    topic,
                    duration.toMillis()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            recordFailure(
                    message,
                    topic,
                    startedAt,
                    exception
            );

            throw new KafkaOutboxPublishException(
                    message.eventId(),
                    exception
            );
        } catch (Exception exception) {
            recordFailure(
                    message,
                    topic,
                    startedAt,
                    exception
            );

            throw new KafkaOutboxPublishException(
                    message.eventId(),
                    exception
            );
        }
    }

    private void recordFailure(
            OutboxMessage message,
            String topic,
            long startedAt,
            Exception exception
    ) {
        Duration duration = elapsed(startedAt);

        observation.recordPublish(
                message.eventType(),
                topic,
                "failure",
                duration
        );

        log.warn(
                "event=outbox_publish_failure event_id={} event_type={} topic={} duration_ms={} error_type={}",
                message.eventId(),
                message.eventType(),
                topic,
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