package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaTopicProvisioningPropertiesTest {

    @Test
    void shouldCreateValidProperties() {
        KafkaTopicProvisioningProperties properties =
                new KafkaTopicProvisioningProperties(
                        true,
                        Duration.ofSeconds(10)
                );

        assertThat(properties.enabled())
                .isTrue();

        assertThat(properties.timeout())
                .isEqualTo(
                        Duration.ofSeconds(10)
                );
    }

    @Test
    void shouldRejectNullTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaTopicProvisioningProperties(
                        true,
                        null
                )
        );
    }

    @Test
    void shouldRejectZeroTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaTopicProvisioningProperties(
                        true,
                        Duration.ZERO
                )
        );
    }

    @Test
    void shouldRejectNegativeTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaTopicProvisioningProperties(
                        true,
                        Duration.ofSeconds(-1)
                )
        );
    }
}