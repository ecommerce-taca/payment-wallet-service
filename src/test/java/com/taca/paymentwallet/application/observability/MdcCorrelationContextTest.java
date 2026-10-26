package com.taca.paymentwallet.infrastructure.observability;

import com.taca.paymentwallet.application.metadata.RequestMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class MdcCorrelationContextTest {

    @AfterEach
    void tearDown() {
        MdcCorrelationContext.clear();
    }

    @Test
    void shouldBindCorrelationMetadata() {
        RequestMetadata metadata = new RequestMetadata(
                "req-001",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                null
        );

        MdcCorrelationContext.bind(metadata);

        assertThat(MDC.get("request_id"))
                .isEqualTo("req-001");

        assertThat(MDC.get("trace_id"))
                .isEqualTo(
                        "4bf92f3577b34da6a3ce929d0e0e4736"
                );

        assertThat(MDC.get("span_id"))
                .isEqualTo(
                        "00f067aa0ba902b7"
                );
    }

    @Test
    void shouldIgnoreMissingRequestId() {
        MdcCorrelationContext.bind(
                new RequestMetadata(
                        null,
                        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                        null
                )
        );

        assertThat(MDC.get("request_id"))
                .isNull();

        assertThat(MDC.get("trace_id"))
                .isNotNull();
    }

    @Test
    void shouldClearCorrelationMetadata() {
        MdcCorrelationContext.bind(
                new RequestMetadata(
                        "req-001",
                        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                        null
                )
        );

        MdcCorrelationContext.clear();

        assertThat(MDC.get("request_id")).isNull();
        assertThat(MDC.get("trace_id")).isNull();
        assertThat(MDC.get("span_id")).isNull();
    }
}