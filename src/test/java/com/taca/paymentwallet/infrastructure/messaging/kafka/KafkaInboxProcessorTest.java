package com.taca.paymentwallet.infrastructure.messaging.kafka;

import com.taca.paymentwallet.application.command.ProcessInboxEventCommand;
import com.taca.paymentwallet.application.inbox.InboxEventProcessingAction;
import com.taca.paymentwallet.application.metadata.RequestMetadataContext;
import com.taca.paymentwallet.application.port.in.ProcessInboxEventUseCase;
import com.taca.paymentwallet.application.result.InboxEventExecutionResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaInboxProcessorTest {

    @AfterEach
    void tearDown() {
        RequestMetadataContext.clear();
    }

    @Test
    void shouldBuildInboxCommandAndExecuteAction() {
        CapturingInboxUseCase useCase =
                new CapturingInboxUseCase();

        KafkaInboxProcessor processor =
                new KafkaInboxProcessor(
                        useCase,
                        new KafkaInboundMetadataExtractor(),
                        new KafkaPayloadHasher()
                );

        ConsumerRecord<String, String> record =
                record("{\"shipment_id\":\"shipment-1\"}");

        addHeader(
                record,
                KafkaHeaderNames.EVENT_ID,
                "event-001"
        );

        AtomicBoolean actionExecuted =
                new AtomicBoolean(false);

        InboxEventExecutionResult<String> result =
                processor.process(
                        "payment-wallet-shipment-consumer",
                        "shipment-service",
                        "shipment.delivered",
                        record,
                        () -> {
                            actionExecuted.set(true);
                            return "done";
                        }
                );

        assertThat(actionExecuted).isTrue();

        assertThat(useCase.command.consumerName())
                .isEqualTo(
                        "payment-wallet-shipment-consumer"
                );

        assertThat(useCase.command.source())
                .isEqualTo("shipment-service");

        assertThat(useCase.command.eventId())
                .isEqualTo("event-001");

        assertThat(useCase.command.eventType())
                .isEqualTo("shipment.delivered");

        assertThat(useCase.command.payloadHash())
                .hasSize(64);

        assertThat(result.value())
                .isEqualTo("done");
    }

    @Test
    void shouldExposeTracingMetadataDuringActionAndClearAfterwards() {
        CapturingInboxUseCase useCase =
                new CapturingInboxUseCase();

        KafkaInboxProcessor processor =
                new KafkaInboxProcessor(
                        useCase,
                        new KafkaInboundMetadataExtractor(),
                        new KafkaPayloadHasher()
                );

        ConsumerRecord<String, String> record =
                record("{}");

        addHeader(
                record,
                KafkaHeaderNames.EVENT_ID,
                "event-001"
        );

        addHeader(
                record,
                KafkaHeaderNames.REQUEST_ID,
                "req-001"
        );

        addHeader(
                record,
                KafkaHeaderNames.TRACEPARENT,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );

        processor.process(
                "consumer",
                "shipment-service",
                "shipment.delivered",
                record,
                () -> {
                    var metadata =
                            RequestMetadataContext.current()
                                    .orElseThrow();

                    assertThat(metadata.requestId())
                            .isEqualTo("req-001");

                    assertThat(metadata.traceparent())
                            .startsWith("00-");

                    return null;
                }
        );

        assertThat(
                RequestMetadataContext.current()
        ).isEmpty();
    }

    private ConsumerRecord<String, String> record(
            String payload
    ) {
        return new ConsumerRecord<>(
                "test-topic",
                0,
                1L,
                "key",
                payload
        );
    }

    private void addHeader(
            ConsumerRecord<String, String> record,
            String name,
            String value
    ) {
        record.headers().add(
                name,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static final class CapturingInboxUseCase
            implements ProcessInboxEventUseCase {

        private ProcessInboxEventCommand command;

        @Override
        public <T> InboxEventExecutionResult<T> execute(
                ProcessInboxEventCommand command,
                Supplier<T> action
        ) {
            this.command = command;

            return new InboxEventExecutionResult<>(
                    InboxEventProcessingAction.APPLIED,
                    action.get()
            );
        }
    }
}