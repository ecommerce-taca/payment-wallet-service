package com.taca.paymentwallet.application.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxMessageTest {

    @Test
    void shouldCreateValidMessage() {
        OutboxMessage message = new OutboxMessage(
                UUID.randomUUID(),
                "PAYMENT",
                UUID.randomUUID(),
                "payment.succeeded",
                "{\"amount\":100000}",
                null,
                Instant.parse("2026-09-30T10:00:00Z"),
                0
        );

        assertThat(message.retryCount()).isZero();
        assertThat(message.eventType()).isEqualTo("payment.succeeded");
    }

    @Test
    void shouldRejectNegativeRetryCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxMessage(
                        UUID.randomUUID(),
                        "PAYMENT",
                        UUID.randomUUID(),
                        "payment.succeeded",
                        "{}",
                        null,
                        Instant.now(),
                        -1
                )
        );
    }
}