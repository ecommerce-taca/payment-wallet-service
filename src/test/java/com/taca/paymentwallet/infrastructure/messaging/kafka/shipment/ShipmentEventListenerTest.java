package com.taca.paymentwallet.infrastructure.messaging.kafka.shipment;

import com.taca.paymentwallet.application.command.ProcessCodPaymentCommand;
import com.taca.paymentwallet.application.command.ProcessInboxEventCommand;
import com.taca.paymentwallet.application.inbox.InboxEventProcessingAction;
import com.taca.paymentwallet.application.payment.CodPaymentResultStatus;
import com.taca.paymentwallet.application.port.in.ProcessCodPaymentUseCase;
import com.taca.paymentwallet.application.port.in.ProcessInboxEventUseCase;
import com.taca.paymentwallet.application.result.InboxEventExecutionResult;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaInboundMetadataExtractor;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaInboxProcessor;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaPayloadHasher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShipmentEventListenerTest {

    private final CapturingInboxUseCase inboxUseCase = new CapturingInboxUseCase();

    private final KafkaInboxProcessor inboxProcessor = new KafkaInboxProcessor(
            inboxUseCase,
            new KafkaInboundMetadataExtractor(),
            new KafkaPayloadHasher()
    );

    private final ProcessCodPaymentUseCase processCodPaymentUseCase =
            mock(ProcessCodPaymentUseCase.class);

    private final ShipmentEventListener listener = new ShipmentEventListener(
            new ShipmentEventParser(new ObjectMapper()),
            inboxProcessor,
            processCodPaymentUseCase
    );

    @Test
    void shouldMapShipmentDeliveredToCodDelivered() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        String payload = deliveredEvent(
                eventId,
                orderId,
                "2026-10-04T05:00:00Z",
                "2026-10-04T04:55:00Z"
        );

        listener.consume(record(payload));

        ArgumentCaptor<ProcessCodPaymentCommand> captor =
                ArgumentCaptor.forClass(ProcessCodPaymentCommand.class);

        verify(processCodPaymentUseCase).execute(captor.capture());

        ProcessCodPaymentCommand command = captor.getValue();

        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.status()).isEqualTo(CodPaymentResultStatus.DELIVERED);
        assertThat(command.occurredAt())
                .isEqualTo(Instant.parse("2026-10-04T04:55:00Z"));
        assertThat(command.failureCode()).isNull();

        assertThat(inboxUseCase.command.eventId())
                .isEqualTo(eventId.toString());
        assertThat(inboxUseCase.command.eventType())
                .isEqualTo("shipment.delivered");
    }

    @Test
    void shouldMapShipmentFailedToCodFailed() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        String payload = failedEvent(
                eventId,
                orderId,
                "2026-10-04T05:00:00Z"
        );

        listener.consume(record(payload));

        ArgumentCaptor<ProcessCodPaymentCommand> captor =
                ArgumentCaptor.forClass(ProcessCodPaymentCommand.class);

        verify(processCodPaymentUseCase).execute(captor.capture());

        ProcessCodPaymentCommand command = captor.getValue();

        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.status()).isEqualTo(CodPaymentResultStatus.FAILED);
        assertThat(command.occurredAt())
                .isEqualTo(Instant.parse("2026-10-04T05:00:00Z"));
        assertThat(command.failureCode()).isEqualTo("SHIPMENT_FAILED");
    }

    @Test
    void shouldUseEnvelopeEventIdWhenKafkaHeaderIsMissing() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        listener.consume(record(deliveredEvent(
                eventId,
                orderId,
                "2026-10-04T05:00:00Z",
                "2026-10-04T04:55:00Z"
        )));

        assertThat(inboxUseCase.command.eventId())
                .isEqualTo(eventId.toString());
    }

    @Test
    void shouldRejectUnsupportedSchemaVersion() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        String payload = """
                {
                  "event_id":"%s",
                  "schema_version":2,
                  "event_type":"shipment.delivered",
                  "occurred_at":"2026-10-04T05:00:00Z",
                  "aggregate_type":"SHIPMENT",
                  "aggregate_id":"%s",
                  "payload":{
                    "order_id":"%s",
                    "delivered_at":"2026-10-04T04:55:00Z"
                  }
                }
                """.formatted(eventId, UUID.randomUUID(), orderId);

        assertThatThrownBy(() -> listener.consume(record(payload)))
                .isInstanceOf(InvalidShipmentEventException.class)
                .hasMessageContaining("Unsupported shipment schema version");

        verifyNoInteractions(processCodPaymentUseCase);
    }

    private ConsumerRecord<String, String> record(String payload) {
        return new ConsumerRecord<>(
                "shipment.events.v1",
                0,
                1L,
                "order-key",
                payload
        );
    }

    private String deliveredEvent(
            UUID eventId,
            UUID orderId,
            String occurredAt,
            String deliveredAt
    ) {
        return """
                {
                  "event_id":"%s",
                  "schema_version":1,
                  "event_type":"shipment.delivered",
                  "occurred_at":"%s",
                  "aggregate_type":"SHIPMENT",
                  "aggregate_id":"%s",
                  "payload":{
                    "shipment_id":"%s",
                    "order_id":"%s",
                    "delivered_at":"%s"
                  }
                }
                """.formatted(
                eventId,
                occurredAt,
                UUID.randomUUID(),
                UUID.randomUUID(),
                orderId,
                deliveredAt
        );
    }

    private String failedEvent(
            UUID eventId,
            UUID orderId,
            String occurredAt
    ) {
        return """
                {
                  "event_id":"%s",
                  "schema_version":1,
                  "event_type":"shipment.failed",
                  "occurred_at":"%s",
                  "aggregate_type":"SHIPMENT",
                  "aggregate_id":"%s",
                  "payload":{
                    "shipment_id":"%s",
                    "order_id":"%s",
                    "reason":"DELIVERY_FAILED",
                    "source":"GHN"
                  }
                }
                """.formatted(
                eventId,
                occurredAt,
                UUID.randomUUID(),
                UUID.randomUUID(),
                orderId
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