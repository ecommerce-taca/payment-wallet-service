package com.taca.paymentwallet.infrastructure.persistence.support;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PersistenceUuidGeneratorTest {

    private final PersistenceUuidGenerator generator =
            new PersistenceUuidGenerator();

    @Test
    void shouldGenerateUuidVersion7() {
        UUID id = generator.next(
                Instant.parse(
                        "2026-09-24T12:00:00Z"
                )
        );

        assertEquals(
                7,
                id.version()
        );

        assertEquals(
                2,
                id.variant()
        );
    }

    @Test
    void shouldGenerateDifferentIdsAtSameTimestamp() {
        Instant now =
                Instant.parse(
                        "2026-09-24T12:00:00Z"
                );

        UUID first =
                generator.next(now);

        UUID second =
                generator.next(now);

        assertNotEquals(
                first,
                second
        );
    }
}