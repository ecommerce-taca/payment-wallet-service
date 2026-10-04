package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.infrastructure.messaging.kafka.AdminClientKafkaTopicNamesClient;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaTopicProperties;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaTopicProvisioningGuard;
import com.taca.paymentwallet.infrastructure.messaging.kafka.KafkaTopicProvisioningProperties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Testcontainers(disabledWithoutDocker = true)
class KafkaTopicProvisioningIntegrationTest {

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka:3.8.0"
                    )
            );

    private AdminClient adminClient;

    @BeforeEach
    void setUp() {
        adminClient = AdminClient.create(
                Map.of(
                        "bootstrap.servers",
                        KAFKA.getBootstrapServers()
                )
        );
    }

    @AfterEach
    void tearDown() {
        adminClient.close();
    }

    @Test
    void shouldPassWhenAllRequiredTopicsExist()
            throws Exception {

        KafkaTopicProperties topics =
                uniqueTopicProperties();

        adminClient.createTopics(
                        List.of(
                                topic(
                                        topics.paymentEvents()
                                ),
                                topic(
                                        topics.walletEvents()
                                ),
                                topic(
                                        topics.shipmentEvents()
                                ),
                                topic(
                                        topics.outboxDlq()
                                )
                        )
                )
                .all()
                .get();

        KafkaTopicProvisioningGuard guard =
                guard(topics);

        assertDoesNotThrow(
                () -> guard.run(null)
        );
    }

    @Test
    void shouldFailWhenShipmentTopicIsMissing()
            throws Exception {

        KafkaTopicProperties topics =
                uniqueTopicProperties();

        adminClient.createTopics(
                        List.of(
                                topic(
                                        topics.paymentEvents()
                                ),
                                topic(
                                        topics.walletEvents()
                                ),
                                topic(
                                        topics.outboxDlq()
                                )
                        )
                )
                .all()
                .get();

        KafkaTopicProvisioningGuard guard =
                guard(topics);

        assertThatThrownBy(
                () -> guard.run(null)
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        topics.shipmentEvents()
                );
    }

    private KafkaTopicProvisioningGuard guard(
            KafkaTopicProperties topics
    ) {
        return new KafkaTopicProvisioningGuard(
                topics,
                new KafkaTopicProvisioningProperties(
                        true,
                        Duration.ofSeconds(10)
                ),
                new AdminClientKafkaTopicNamesClient(
                        adminClient
                )
        );
    }

    private KafkaTopicProperties uniqueTopicProperties() {
        String suffix =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        return new KafkaTopicProperties(
                "payment.events." + suffix,
                "wallet.events." + suffix,
                "shipment.events." + suffix,
                "payment-wallet.outbox.dlq." + suffix
        );
    }

    private NewTopic topic(
            String name
    ) {
        return new NewTopic(
                name,
                1,
                (short) 1
        );
    }
}