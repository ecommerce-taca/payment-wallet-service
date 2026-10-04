package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaHealthPropertiesTest {

    @Test
    void shouldCreateValidProperties() {
        KafkaHealthProperties properties =
                new KafkaHealthProperties(
                        true,
                        Duration.ofSeconds(5)
                );

        assertThat(properties.enabled())
                .isTrue();

        assertThat(properties.timeout())
                .isEqualTo(
                        Duration.ofSeconds(5)
                );
    }

    @Test
    void shouldRejectNullTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaHealthProperties(
                        true,
                        null
                )
        );
    }

    @Test
    void shouldRejectZeroTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaHealthProperties(
                        true,
                        Duration.ZERO
                )
        );
    }

    @Test
    void shouldRejectNegativeTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaHealthProperties(
                        true,
                        Duration.ofSeconds(-1)
                )
        );
    }
}