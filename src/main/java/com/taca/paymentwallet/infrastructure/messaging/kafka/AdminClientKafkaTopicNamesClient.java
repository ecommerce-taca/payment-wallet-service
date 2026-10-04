package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.apache.kafka.clients.admin.AdminClient;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AdminClientKafkaTopicNamesClient
        implements KafkaTopicNamesClient {

    private final AdminClient adminClient;

    public AdminClientKafkaTopicNamesClient(
            AdminClient adminClient
    ) {
        this.adminClient =
                Objects.requireNonNull(adminClient);
    }

    @Override
    public Set<String> listTopicNames(
            Duration timeout
    ) {
        Objects.requireNonNull(
                timeout,
                "timeout must not be null"
        );

        if (timeout.isZero()
                || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must be positive"
            );
        }

        try {
            return adminClient
                    .listTopics()
                    .names()
                    .get(
                            timeout.toMillis(),
                            TimeUnit.MILLISECONDS
                    );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new KafkaTopicProvisioningException(
                    "Interrupted while listing Kafka topics",
                    exception
            );
        } catch (Exception exception) {
            throw new KafkaTopicProvisioningException(
                    "Failed to list Kafka topics",
                    exception
            );
        }
    }
}