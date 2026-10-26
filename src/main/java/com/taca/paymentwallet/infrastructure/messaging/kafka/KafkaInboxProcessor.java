package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.command.ProcessInboxEventCommand;
import com.taca.paymentwallet.application.metadata.RequestMetadata;
import com.taca.paymentwallet.application.metadata.RequestMetadataContext;
import com.taca.paymentwallet.application.port.in.ProcessInboxEventUseCase;
import com.taca.paymentwallet.application.result.InboxEventExecutionResult;
import com.taca.paymentwallet.infrastructure.observability.MdcCorrelationContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

public class KafkaInboxProcessor {

    private final ProcessInboxEventUseCase processInboxEventUseCase;
    private final KafkaInboundMetadataExtractor metadataExtractor;
    private final KafkaPayloadHasher payloadHasher;
    private final KafkaConsumerObservation observation;

    public KafkaInboxProcessor(
            ProcessInboxEventUseCase processInboxEventUseCase,
            KafkaInboundMetadataExtractor metadataExtractor,
            KafkaPayloadHasher payloadHasher,
            KafkaConsumerObservation observation
    ) {
        this.processInboxEventUseCase =
                Objects.requireNonNull(processInboxEventUseCase);
        this.metadataExtractor = Objects.requireNonNull(metadataExtractor);
        this.payloadHasher = Objects.requireNonNull(payloadHasher);
        this.observation = Objects.requireNonNull(observation);
    }

    public <T> InboxEventExecutionResult<T> process(
            String consumerName,
            String source,
            String eventType,
            ConsumerRecord<String, String> record,
            Supplier<T> action
    ) {
        return process(
                consumerName,
                source,
                eventType,
                record,
                null,
                action
        );
    }

    public <T> InboxEventExecutionResult<T> process(
            String consumerName,
            String source,
            String eventType,
            ConsumerRecord<String, String> record,
            String fallbackEventId,
            Supplier<T> action
    ) {
        requireText(consumerName, "consumerName");
        requireText(source, "source");
        requireText(eventType, "eventType");
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(action, "action must not be null");

        String payload = Objects.requireNonNull(
                record.value(),
                "Kafka record value must not be null"
        );

        KafkaInboundMetadata metadata =
                metadataExtractor.extract(record, fallbackEventId);

        RequestMetadata requestMetadata =
                new RequestMetadata(
                        metadata.requestId(),
                        metadata.traceparent(),
                        metadata.tracestate()
                );

        long startedAt = System.nanoTime();

        try {
            RequestMetadataContext.set(requestMetadata);
            MdcCorrelationContext.bind(requestMetadata);

            ProcessInboxEventCommand command =
                    new ProcessInboxEventCommand(
                            consumerName,
                            source,
                            metadata.eventId(),
                            eventType,
                            payloadHasher.hash(payload)
                    );

            InboxEventExecutionResult<T> result =
                    processInboxEventUseCase.execute(command, action);

            observation.record(
                    consumerName,
                    eventType,
                    record.topic(),
                    result.isDuplicate() ? "duplicate" : "success",
                    elapsed(startedAt)
            );

            return result;
        } catch (RuntimeException exception) {
            observation.record(
                    consumerName,
                    eventType,
                    record.topic(),
                    "failure",
                    elapsed(startedAt)
            );

            throw exception;
        } finally {
            MdcCorrelationContext.clear();
            RequestMetadataContext.clear();
        }
    }

    private Duration elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }

    private void requireText(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " must not be blank"
            );
        }
    }
}