package com.taca.paymentwallet.application.command;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateAdminTaxConfigCommandTest {

    private static final Instant EFFECTIVE_FROM =
            Instant.parse("2026-11-01T00:00:00Z");

    private static final UUID ACTOR_ID =
            UUID.randomUUID();

    @Test
    void shouldNormalizePlatformScope() {
        CreateAdminTaxConfigCommand command =
                new CreateAdminTaxConfigCommand(
                        " platform ",
                        null,
                        100,
                        EFFECTIVE_FROM,
                        "Platform tax",
                        ACTOR_ID
                );

        assertEquals("PLATFORM", command.scope());
    }

    @Test
    void shouldRequireCategoryForCategoryScope() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateAdminTaxConfigCommand(
                        "CATEGORY",
                        null,
                        100,
                        EFFECTIVE_FROM,
                        null,
                        ACTOR_ID
                )
        );
    }

    @Test
    void shouldRejectCategoryForPlatformScope() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateAdminTaxConfigCommand(
                        "PLATFORM",
                        UUID.randomUUID(),
                        100,
                        EFFECTIVE_FROM,
                        null,
                        ACTOR_ID
                )
        );
    }

    @Test
    void shouldRejectInvalidRate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateAdminTaxConfigCommand(
                        "PLATFORM",
                        null,
                        10_001,
                        EFFECTIVE_FROM,
                        null,
                        ACTOR_ID
                )
        );
    }
}