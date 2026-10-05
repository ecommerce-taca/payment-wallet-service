package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class KafkaTopicProvisioningGuard
        implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(
                    KafkaTopicProvisioningGuard.class
            );

    private final KafkaTopicProperties topicProperties;
    private final KafkaTopicProvisioningProperties properties;
    private final KafkaTopicNamesClient topicNamesClient;

    public KafkaTopicProvisioningGuard(
            KafkaTopicProperties topicProperties,
            KafkaTopicProvisioningProperties properties,
            KafkaTopicNamesClient topicNamesClient
    ) {
        this.topicProperties =
                Objects.requireNonNull(topicProperties);

        this.properties =
                Objects.requireNonNull(properties);

        this.topicNamesClient =
                Objects.requireNonNull(topicNamesClient);
    }

    @Override
    public void run(
            ApplicationArguments args
    ) {
        if (!properties.enabled()) {
            return;
        }

        Set<String> requiredTopics =
                Set.of(
                        topicProperties.paymentEvents(),
                        topicProperties.walletEvents(),
                        topicProperties.shipmentEvents(),
                        topicProperties.outboxDlq()
                );

        Set<String> existingTopics =
                topicNamesClient.listTopicNames(
                        properties.timeout()
                );

        Set<String> missingTopics =
                new TreeSet<>(requiredTopics);

        missingTopics.removeAll(existingTopics);

        if (!missingTopics.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required Kafka topic(s): "
                            + String.join(
                            ", ",
                            missingTopics
                    )
            );
        }

        log.info(
                "event=kafka_topic_provisioning_check result=success required_topic_count={}",
                requiredTopics.size()
        );
    }
}