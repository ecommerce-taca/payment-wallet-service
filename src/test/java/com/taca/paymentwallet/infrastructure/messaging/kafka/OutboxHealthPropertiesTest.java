package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxHealthPropertiesTest {

    @Test
    void shouldCreateValidProperties() {
        OutboxHealthProperties properties =
                new OutboxHealthProperties(
                        true,
                        Duration.ofMinutes(5)
                );

        assertThat(properties.enabled())
                .isTrue();

        assertThat(properties.maxLag())
                .isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldRejectNullMaxLag() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxHealthProperties(
                        true,
                        null
                )
        );
    }

    @Test
    void shouldRejectZeroMaxLag() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxHealthProperties(
                        true,
                        Duration.ZERO
                )
        );
    }

    @Test
    void shouldRejectNegativeMaxLag() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxHealthProperties(
                        true,
                        Duration.ofSeconds(-1)
                )
        );
    }
}