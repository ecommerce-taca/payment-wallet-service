package com.taca.paymentwallet.application.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalCallerContextTest {

    @Test
    void shouldCreateInternalCallerContext() {
        InternalCallerContext context =
                new InternalCallerContext(InternalService.ORDER_COMMERCE);

        assertThat(context.service()).isEqualTo(InternalService.ORDER_COMMERCE);
    }

    @Test
    void shouldRejectNullService() {
        assertThatThrownBy(() -> new InternalCallerContext(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("service must not be null");
    }
}