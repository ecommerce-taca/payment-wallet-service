package com.taca.paymentwallet.application.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InternalCallerContextHolderTest {

    @AfterEach
    void tearDown() {
        InternalCallerContextHolder.clear();
    }

    @Test
    void shouldExposeCurrentCaller() {
        InternalCallerContext context =
                new InternalCallerContext(InternalService.ORDER_COMMERCE);

        InternalCallerContextHolder.set(context);

        assertThat(InternalCallerContextHolder.current()).containsSame(context);
    }

    @Test
    void shouldClearContext() {
        InternalCallerContextHolder.set(
                new InternalCallerContext(InternalService.ORDER_COMMERCE)
        );

        InternalCallerContextHolder.clear();

        assertThat(InternalCallerContextHolder.current()).isEmpty();
    }

    @Test
    void shouldClearWhenNullIsSet() {
        InternalCallerContextHolder.set(
                new InternalCallerContext(InternalService.ORDER_COMMERCE)
        );

        InternalCallerContextHolder.set(null);

        assertThat(InternalCallerContextHolder.current()).isEmpty();
    }
}