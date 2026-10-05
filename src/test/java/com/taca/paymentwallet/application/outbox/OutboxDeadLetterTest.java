package com.taca.paymentwallet.application.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxDeadLetterTest {

    @Test
    void shouldCreateValidDeadLetter() {
        UUID eventId = UUID.randomUUID();

        OutboxDeadLetter deadLetter = new OutboxDeadLetter(
                eventId,
                "PAYMENT",
                UUID.randomUUID(),
                "payment.created",
                "{}",
                null,
                Instant.parse("2026-10-01T10:00:00Z"),
                3,
                "Kafka unavailable"
        );

        assertThat(deadLetter.eventId()).isEqualTo(eventId);
        assertThat(deadLetter.retryCount()).isEqualTo(3);
        assertThat(deadLetter.lastError()).isEqualTo("Kafka unavailable");
    }

    @Test
    void shouldRejectNonPositiveRetryCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxDeadLetter(
                        UUID.randomUUID(),
                        "PAYMENT",
                        UUID.randomUUID(),
                        "payment.created",
                        "{}",
                        null,
                        Instant.now(),
                        0,
                        "Kafka unavailable"
                )
        );
    }

    @Test
    void shouldRejectBlankLastError() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxDeadLetter(
                        UUID.randomUUID(),
                        "PAYMENT",
                        UUID.randomUUID(),
                        "payment.created",
                        "{}",
                        null,
                        Instant.now(),
                        3,
                        " "
                )
        );
    }
}