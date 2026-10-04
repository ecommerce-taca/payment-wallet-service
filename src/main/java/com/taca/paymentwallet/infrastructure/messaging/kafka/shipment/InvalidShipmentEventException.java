package com.taca.paymentwallet.infrastructure.messaging.kafka.shipment;

public class InvalidShipmentEventException extends RuntimeException {

    public InvalidShipmentEventException(String message) {
        super(message);
    }

    public InvalidShipmentEventException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}