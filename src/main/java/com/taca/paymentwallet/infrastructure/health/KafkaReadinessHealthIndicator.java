package com.taca.paymentwallet.infrastructure.health;

import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaHealthProperties;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaTopicNamesClient;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaTopicProperties;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class KafkaReadinessHealthIndicator
        extends AbstractHealthIndicator {

    private final KafkaTopicNamesClient topicNamesClient;
    private final KafkaTopicProperties topicProperties;
    private final KafkaHealthProperties properties;

    public KafkaReadinessHealthIndicator(
            KafkaTopicNamesClient topicNamesClient,
            KafkaTopicProperties topicProperties,
            KafkaHealthProperties properties
    ) {
        this.topicNamesClient =
                Objects.requireNonNull(topicNamesClient);

        this.topicProperties =
                Objects.requireNonNull(topicProperties);

        this.properties =
                Objects.requireNonNull(properties);
    }

    @Override
    protected void doHealthCheck(
            Health.Builder builder
    ) {
        if (!properties.enabled()) {
            builder.up()
                    .withDetail("enabled", false);

            return;
        }

        try {
            Set<String> existingTopics =
                    topicNamesClient.listTopicNames(
                            properties.timeout()
                    );

            Set<String> requiredTopics =
                    requiredTopics();

            Set<String> missingTopics =
                    new TreeSet<>(requiredTopics);

            missingTopics.removeAll(existingTopics);

            if (!missingTopics.isEmpty()) {
                builder.outOfService()
                        .withDetail("enabled", true)
                        .withDetail(
                                "requiredTopicCount",
                                requiredTopics.size()
                        )
                        .withDetail(
                                "missingTopics",
                                missingTopics
                        );

                return;
            }

            builder.up()
                    .withDetail("enabled", true)
                    .withDetail(
                            "requiredTopicCount",
                            requiredTopics.size()
                    );
        } catch (RuntimeException exception) {
            builder.outOfService()
                    .withDetail("enabled", true)
                    .withDetail(
                            "reason",
                            "kafka_unavailable"
                    )
                    .withDetail(
                            "errorType",
                            exception
                                    .getClass()
                                    .getSimpleName()
                    );
        }
    }

    private Set<String> requiredTopics() {
        return Set.of(
                topicProperties.paymentEvents(),
                topicProperties.walletEvents(),
                topicProperties.shipmentEvents(),
                topicProperties.outboxDlq()
        );
    }
}