package com.taca.paymentwallet.application.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActorContextHolderTest {

    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    @Test
    void shouldExposeCurrentActorContext() {
        ActorContext actor = actor();

        ActorContextHolder.set(actor);

        assertThat(ActorContextHolder.current())
                .containsSame(actor);
    }

    @Test
    void shouldBeEmptyWhenContextWasNotSet() {
        assertThat(ActorContextHolder.current())
                .isEmpty();
    }

    @Test
    void shouldClearActorContext() {
        ActorContextHolder.set(actor());

        ActorContextHolder.clear();

        assertThat(ActorContextHolder.current())
                .isEmpty();
    }

    @Test
    void shouldClearContextWhenNullIsSet() {
        ActorContextHolder.set(actor());

        ActorContextHolder.set(null);

        assertThat(ActorContextHolder.current())
                .isEmpty();
    }

    @Test
    void shouldIsolateContextBetweenThreads() throws Exception {
        ActorContext actor = actor();
        ActorContextHolder.set(actor);

        Thread thread = new Thread(() ->
                assertThat(ActorContextHolder.current()).isEmpty()
        );

        thread.start();
        thread.join();

        assertThat(ActorContextHolder.current())
                .containsSame(actor);
    }

    private ActorContext actor() {
        return new ActorContext(
                USER_ID,
                Set.of("SELLER"),
                Set.of("FINANCE_OPS"),
                Set.of(SHOP_ID)
        );
    }
}