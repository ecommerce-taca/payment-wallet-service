package com.taca.paymentwallet.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaPayloadHasherTest {

    private final KafkaPayloadHasher hasher =
            new KafkaPayloadHasher();

    @Test
    void shouldProduceDeterministicSha256Hash() {
        String first = hasher.hash(
                "{\"event\":\"shipment.delivered\"}"
        );

        String second = hasher.hash(
                "{\"event\":\"shipment.delivered\"}"
        );

        assertThat(first)
                .isEqualTo(second);

        assertThat(first)
                .hasSize(64);
    }

    @Test
    void shouldProduceDifferentHashesForDifferentPayloads() {
        assertThat(
                hasher.hash("{\"value\":1}")
        ).isNotEqualTo(
                hasher.hash("{\"value\":2}")
        );
    }
}