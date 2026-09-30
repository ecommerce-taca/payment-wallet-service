package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaTopicPropertiesTest {

    @Test
    void shouldCreateValidTopicProperties() {
        KafkaTopicProperties properties =
                new KafkaTopicProperties("payment.events.v1", "wallet.events.v1");

        assertThat(properties.paymentEvents()).isEqualTo("payment.events.v1");
        assertThat(properties.walletEvents()).isEqualTo("wallet.events.v1");
    }

    @Test
    void shouldRejectBlankPaymentTopic() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaTopicProperties(" ", "wallet.events.v1")
        );
    }

    @Test
    void shouldRejectBlankWalletTopic() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaTopicProperties("payment.events.v1", " ")
        );
    }
}