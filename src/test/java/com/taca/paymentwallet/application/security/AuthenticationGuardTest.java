package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationGuardTest {

    private final AuthenticationGuard guard = new AuthenticationGuard();

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    @Test
    void shouldReturnCurrentActor() {
        ActorContext actor = new ActorContext(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                Set.of("SELLER"),
                Set.of(),
                Set.of()
        );

        ActorContextHolder.set(actor);

        assertThat(guard.requireActor()).isSameAs(actor);
    }

    @Test
    void shouldRejectMissingActor() {
        assertThatThrownBy(guard::requireActor)
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("Authentication is required");
    }
}