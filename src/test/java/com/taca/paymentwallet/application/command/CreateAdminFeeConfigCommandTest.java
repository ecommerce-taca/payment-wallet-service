package com.taca.paymentwallet.application.command;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateAdminFeeConfigCommandTest {

    private static final Instant EFFECTIVE_FROM =
            Instant.parse("2026-11-01T00:00:00Z");

    private static final UUID ACTOR_ID =
            UUID.randomUUID();

    @Test
    void shouldNormalizePlatformScope() {
        CreateAdminFeeConfigCommand command =
                new CreateAdminFeeConfigCommand(
                        " platform ",
                        null,
                        700,
                        EFFECTIVE_FROM,
                        "Platform commission",
                        ACTOR_ID
                );

        assertEquals("PLATFORM", command.scope());
    }

    @Test
    void shouldRequireCategoryForCategoryScope() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateAdminFeeConfigCommand(
                        "CATEGORY",
                        null,
                        700,
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
                () -> new CreateAdminFeeConfigCommand(
                        "PLATFORM",
                        UUID.randomUUID(),
                        700,
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
                () -> new CreateAdminFeeConfigCommand(
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