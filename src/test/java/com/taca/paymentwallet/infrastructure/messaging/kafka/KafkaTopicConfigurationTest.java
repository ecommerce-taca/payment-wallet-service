package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicConfigurationTest {

    private final KafkaTopicProperties properties = new KafkaTopicProperties(
        "payment.events.v1",
        "wallet.events.v1",
        "payment-wallet.outbox.dlq.v1"
    );

    private final KafkaTopicConfiguration configuration =
            new KafkaTopicConfiguration();

    @Test
    void shouldCreatePaymentEventsTopic() {
        NewTopic topic = configuration.paymentEventsTopic(properties);

        assertThat(topic.name()).isEqualTo("payment.events.v1");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void shouldCreateWalletEventsTopic() {
        NewTopic topic = configuration.walletEventsTopic(properties);

        assertThat(topic.name()).isEqualTo("wallet.events.v1");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void shouldCreateOutboxDlqTopic() {
        NewTopic topic =
                configuration.outboxDlqTopic(properties);

        assertThat(topic.name())
                .isEqualTo("payment-wallet.outbox.dlq.v1");

        assertThat(topic.numPartitions())
                .isEqualTo(3);

        assertThat(topic.replicationFactor())
                .isEqualTo((short) 1);
    }
}