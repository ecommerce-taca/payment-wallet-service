package com.taca.paymentwallet.infrastructure.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class W3cTraceContextTest {

    private final W3cTraceContext traceContext =
            new W3cTraceContext();

    @Test
    void shouldCreateValidTraceparent() {
        String value = traceContext.create();

        assertThat(traceContext.isValid(value)).isTrue();
        assertThat(value).hasSize(55);
        assertThat(value).startsWith("00-");
    }

    @Test
    void shouldCreateChildWithSameTraceId() {
        String parent =
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        String child = traceContext.childOf(parent);

        assertThat(traceContext.isValid(child)).isTrue();

        assertThat(child.substring(3, 35))
                .isEqualTo(parent.substring(3, 35));

        assertThat(child.substring(36, 52))
                .isNotEqualTo(parent.substring(36, 52));
    }

    @Test
    void shouldCreateNewTraceForInvalidTraceparent() {
        String value = traceContext.childOf("invalid");

        assertThat(traceContext.isValid(value)).isTrue();
    }

    @Test
    void shouldRejectZeroTraceId() {
        assertThat(traceContext.isValid(
                "00-00000000000000000000000000000000-00f067aa0ba902b7-01"
        )).isFalse();
    }

    @Test
    void shouldRejectZeroParentId() {
        assertThat(traceContext.isValid(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-0000000000000000-01"
        )).isFalse();
    }
}