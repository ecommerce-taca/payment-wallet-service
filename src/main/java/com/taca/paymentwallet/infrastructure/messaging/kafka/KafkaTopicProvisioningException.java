package com.taca.paymentwallet.infrastructure.messaging.kafka;

public class KafkaTopicProvisioningException
        extends RuntimeException {

    public KafkaTopicProvisioningException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}