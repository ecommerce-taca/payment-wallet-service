package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.command.ProcessInboxEventCommand;
import com.taca.paymentwallet.application.metadata.RequestMetadata;
import com.taca.paymentwallet.application.metadata.RequestMetadataContext;
import com.taca.paymentwallet.application.port.in.ProcessInboxEventUseCase;
import com.taca.paymentwallet.application.result.InboxEventExecutionResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Objects;
import java.util.function.Supplier;

public class KafkaInboxProcessor {

    private final ProcessInboxEventUseCase processInboxEventUseCase;
    private final KafkaInboundMetadataExtractor metadataExtractor;
    private final KafkaPayloadHasher payloadHasher;

    public KafkaInboxProcessor(
            ProcessInboxEventUseCase processInboxEventUseCase,
            KafkaInboundMetadataExtractor metadataExtractor,
            KafkaPayloadHasher payloadHasher
    ) {
        this.processInboxEventUseCase =
                Objects.requireNonNull(processInboxEventUseCase);

        this.metadataExtractor =
                Objects.requireNonNull(metadataExtractor);

        this.payloadHasher =
                Objects.requireNonNull(payloadHasher);
    }

    public <T> InboxEventExecutionResult<T> process(
            String consumerName,
            String source,
            String eventType,
            ConsumerRecord<String, String> record,
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

        KafkaInboundMetadata metadata = metadataExtractor.extract(record);

        RequestMetadata requestMetadata =
                new RequestMetadata(
                        metadata.requestId(),
                        metadata.traceparent(),
                        metadata.tracestate()
                );

        try {
            RequestMetadataContext.set(requestMetadata);

            ProcessInboxEventCommand command =
                    new ProcessInboxEventCommand(
                            consumerName,
                            source,
                            metadata.eventId(),
                            eventType,
                            payloadHasher.hash(payload)
                    );

            return processInboxEventUseCase.execute(
                    command,
                    action
            );
        } finally {
            RequestMetadataContext.clear();
        }
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