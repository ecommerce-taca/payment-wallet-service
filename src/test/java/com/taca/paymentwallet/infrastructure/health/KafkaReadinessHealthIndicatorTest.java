package com.taca.paymentwallet.infrastructure.health;

import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaHealthProperties;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaTopicNamesClient;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaTopicProperties;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaTopicProvisioningException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaReadinessHealthIndicatorTest {

    private static final KafkaTopicProperties TOPICS =
            new KafkaTopicProperties(
                    "payment.events.v1",
                    "wallet.events.v1",
                    "shipment.events.v1",
                    "payment-wallet.outbox.dlq.v1"
            );

    private static final KafkaHealthProperties ENABLED =
            new KafkaHealthProperties(
                    true,
                    Duration.ofSeconds(5)
            );

    @Test
    void shouldBeUpWhenAllRequiredTopicsExist() {
        KafkaTopicNamesClient client =
                timeout -> Set.of(
                        "payment.events.v1",
                        "wallet.events.v1",
                        "shipment.events.v1",
                        "payment-wallet.outbox.dlq.v1",
                        "other.topic.v1"
                );

        KafkaReadinessHealthIndicator indicator =
                new KafkaReadinessHealthIndicator(
                        client,
                        TOPICS,
                        ENABLED
                );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(Status.UP);

        assertThat(health.getDetails())
                .containsEntry(
                        "requiredTopicCount",
                        4
                );
    }

    @Test
    void shouldBeOutOfServiceWhenRequiredTopicIsMissing() {
        KafkaTopicNamesClient client =
                timeout -> Set.of(
                        "payment.events.v1",
                        "wallet.events.v1",
                        "payment-wallet.outbox.dlq.v1"
                );

        KafkaReadinessHealthIndicator indicator =
                new KafkaReadinessHealthIndicator(
                        client,
                        TOPICS,
                        ENABLED
                );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(
                        Status.OUT_OF_SERVICE
                );

        assertThat(health.getDetails())
                .containsEntry(
                        "requiredTopicCount",
                        4
                );

        assertThat(
                health.getDetails()
                        .get("missingTopics")
        ).isEqualTo(
                Set.of("shipment.events.v1")
        );
    }

    @Test
    void shouldBeOutOfServiceWhenKafkaIsUnavailable() {
        KafkaTopicNamesClient client =
                timeout -> {
                    throw new KafkaTopicProvisioningException(
                            "Failed to list Kafka topics",
                            new RuntimeException(
                                    "broker unavailable"
                            )
                    );
                };

        KafkaReadinessHealthIndicator indicator =
                new KafkaReadinessHealthIndicator(
                        client,
                        TOPICS,
                        ENABLED
                );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(
                        Status.OUT_OF_SERVICE
                );

        assertThat(health.getDetails())
                .containsEntry(
                        "reason",
                        "kafka_unavailable"
                )
                .containsEntry(
                        "errorType",
                        "KafkaTopicProvisioningException"
                );
    }

    @Test
    void shouldBeUpWithoutQueryingKafkaWhenDisabled() {
        KafkaTopicNamesClient client =
                timeout -> {
                    throw new AssertionError(
                            "Kafka should not be queried"
                    );
                };

        KafkaReadinessHealthIndicator indicator =
                new KafkaReadinessHealthIndicator(
                        client,
                        TOPICS,
                        new KafkaHealthProperties(
                                false,
                                Duration.ofSeconds(5)
                        )
                );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(Status.UP);

        assertThat(health.getDetails())
                .containsEntry(
                        "enabled",
                        false
                );
    }
}