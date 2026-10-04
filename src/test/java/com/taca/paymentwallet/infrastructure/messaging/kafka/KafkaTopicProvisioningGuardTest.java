package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class KafkaTopicProvisioningGuardTest {

    private static final KafkaTopicProperties TOPICS =
            new KafkaTopicProperties(
                    "payment.events.v1",
                    "wallet.events.v1",
                    "shipment.events.v1",
                    "payment-wallet.outbox.dlq.v1"
            );

    private static final KafkaTopicProvisioningProperties ENABLED =
            new KafkaTopicProvisioningProperties(
                    true,
                    Duration.ofSeconds(10)
            );

    @Test
    void shouldPassWhenAllRequiredTopicsExist() {
        KafkaTopicNamesClient client =
                timeout -> Set.of(
                        "payment.events.v1",
                        "wallet.events.v1",
                        "shipment.events.v1",
                        "payment-wallet.outbox.dlq.v1",
                        "other.topic.v1"
                );

        KafkaTopicProvisioningGuard guard =
                new KafkaTopicProvisioningGuard(
                        TOPICS,
                        ENABLED,
                        client
                );

        assertDoesNotThrow(
                () -> guard.run(
                        new DefaultApplicationArguments()
                )
        );
    }

    @Test
    void shouldFailWhenRequiredTopicIsMissing() {
        KafkaTopicNamesClient client =
                timeout -> Set.of(
                        "payment.events.v1",
                        "wallet.events.v1",
                        "payment-wallet.outbox.dlq.v1"
                );

        KafkaTopicProvisioningGuard guard =
                new KafkaTopicProvisioningGuard(
                        TOPICS,
                        ENABLED,
                        client
                );

        assertThatThrownBy(
                () -> guard.run(
                        new DefaultApplicationArguments()
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Missing required Kafka topic(s): shipment.events.v1"
                );
    }

    @Test
    void shouldReportAllMissingTopicsInSortedOrder() {
        KafkaTopicNamesClient client =
                timeout -> Set.of(
                        "payment.events.v1"
                );

        KafkaTopicProvisioningGuard guard =
                new KafkaTopicProvisioningGuard(
                        TOPICS,
                        ENABLED,
                        client
                );

        assertThatThrownBy(
                () -> guard.run(
                        new DefaultApplicationArguments()
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Missing required Kafka topic(s): "
                                + "payment-wallet.outbox.dlq.v1, "
                                + "shipment.events.v1, "
                                + "wallet.events.v1"
                );
    }

    @Test
    void shouldSkipCheckWhenDisabled() {
        KafkaTopicProvisioningProperties disabled =
                new KafkaTopicProvisioningProperties(
                        false,
                        Duration.ofSeconds(10)
                );

        KafkaTopicNamesClient client =
                timeout -> {
                    throw new AssertionError(
                            "Kafka should not be queried"
                    );
                };

        KafkaTopicProvisioningGuard guard =
                new KafkaTopicProvisioningGuard(
                        TOPICS,
                        disabled,
                        client
                );

        assertDoesNotThrow(
                () -> guard.run(
                        new DefaultApplicationArguments()
                )
        );
    }
}