package com.taca.paymentwallet.application.idempotency;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyRecordTest {

    @Test
    void shouldIdentifyProcessingStatus() {
        IdempotencyRecord record = new IdempotencyRecord(
                new IdempotencyScope("PAYMENT:123"),
                "idem-key-1",
                "request-hash-1",
                IdempotencyStatus.PROCESSING,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        assertTrue(record.isProcessing());
        assertFalse(record.isSucceeded());
        assertFalse(record.isFailed());
    }

    @Test
    void shouldIdentifySucceededStatus() {
        IdempotencyRecord record = new IdempotencyRecord(
                new IdempotencyScope("PAYMENT:123"),
                "idem-key-1",
                "request-hash-1",
                IdempotencyStatus.SUCCEEDED,
                "{}",
                null,
                Instant.now(),
                Instant.now()
        );

        assertTrue(record.isSucceeded());
        assertFalse(record.isProcessing());
        assertFalse(record.isFailed());
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IdempotencyRecord(
                        new IdempotencyScope("PAYMENT:123"),
                        " ",
                        "request-hash-1",
                        IdempotencyStatus.PROCESSING,
                        null,
                        null,
                        Instant.now(),
                        Instant.now()
                )
        );
    }

    @Test
    void shouldRejectBlankRequestHash() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IdempotencyRecord(
                        new IdempotencyScope("PAYMENT:123"),
                        "idem-key-1",
                        " ",
                        IdempotencyStatus.PROCESSING,
                        null,
                        null,
                        Instant.now(),
                        Instant.now()
                )
        );
    }
}
