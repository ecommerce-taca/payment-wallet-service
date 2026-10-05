package com.taca.paymentwallet.infrastructure.messaging.kafka.shipment;

import com.taca.paymentwallet.application.command.ProcessCodPaymentCommand;
import com.taca.paymentwallet.application.payment.CodPaymentResultStatus;
import com.taca.paymentwallet.application.port.in.ProcessCodPaymentUseCase;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaInboxProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ShipmentEventListener {

    public static final String CONSUMER_NAME = "payment-wallet-shipment-consumer";
    public static final String SOURCE = "shipment-service";
    static final String SHIPMENT_FAILED = "SHIPMENT_FAILED";
    static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ShipmentEventParser parser;
    private final KafkaInboxProcessor inboxProcessor;
    private final ProcessCodPaymentUseCase processCodPaymentUseCase;

    public ShipmentEventListener(
            ShipmentEventParser parser,
            KafkaInboxProcessor inboxProcessor,
            ProcessCodPaymentUseCase processCodPaymentUseCase
    ) {
        this.parser = Objects.requireNonNull(parser);
        this.inboxProcessor = Objects.requireNonNull(inboxProcessor);
        this.processCodPaymentUseCase = Objects.requireNonNull(processCodPaymentUseCase);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.shipment-events}",
            groupId = "${spring.kafka.consumer.group-id:payment-wallet-service}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        ShipmentEventEnvelope event = parser.parse(record.value());
        validateSchemaVersion(event);

        inboxProcessor.process(
                CONSUMER_NAME,
                SOURCE,
                event.eventType(),
                record,
                event.eventId(),
                () -> process(event)
        );
    }

    private Void process(ShipmentEventEnvelope event) {
        ProcessCodPaymentCommand command = switch (event.eventType()) {
            case ShipmentEventParser.DELIVERED -> deliveredCommand(event);
            case ShipmentEventParser.FAILED -> failedCommand(event);
            default -> throw new InvalidShipmentEventException(
                    "Unsupported shipment event type: " + event.eventType()
            );
        };

        processCodPaymentUseCase.execute(command);
        return null;
    }

    private ProcessCodPaymentCommand deliveredCommand(ShipmentEventEnvelope event) {
        return new ProcessCodPaymentCommand(
                event.orderId(),
                CodPaymentResultStatus.DELIVERED,
                event.deliveredAt(),
                null
        );
    }

    private ProcessCodPaymentCommand failedCommand(ShipmentEventEnvelope event) {
        return new ProcessCodPaymentCommand(
                event.orderId(),
                CodPaymentResultStatus.FAILED,
                event.occurredAt(),
                SHIPMENT_FAILED
        );
    }

    private void validateSchemaVersion(ShipmentEventEnvelope event) {
        if (event.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new InvalidShipmentEventException(
                    "Unsupported shipment schema version: " + event.schemaVersion()
            );
        }
    }
}