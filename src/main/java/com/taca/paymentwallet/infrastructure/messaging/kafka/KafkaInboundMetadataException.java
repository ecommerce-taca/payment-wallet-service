package com.taca.paymentwallet.infrastructure.messaging.kafka;

public class KafkaInboundMetadataException extends RuntimeException {

    public KafkaInboundMetadataException(String message) {
        super(message);
    }
}