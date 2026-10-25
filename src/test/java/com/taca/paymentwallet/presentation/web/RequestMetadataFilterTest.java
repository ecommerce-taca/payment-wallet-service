package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.metadata.RequestMetadata;
import com.taca.paymentwallet.application.metadata.RequestMetadataContext;
import com.taca.paymentwallet.infrastructure.observability.W3cTraceContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMetadataFilterTest {

    private final RequestMetadataFilter filter =
            new RequestMetadataFilter(
                    new W3cTraceContext()
            );

    @AfterEach
    void tearDown() {
        RequestMetadataContext.clear();
    }

    @Test
    void shouldExposeRequestMetadataDuringFilterChain()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "X-Request-ID",
                "req-001"
        );

        request.addHeader(
                "traceparent",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );

        request.addHeader(
                "tracestate",
                "vendor=value"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<RequestMetadata> captured =
                new AtomicReference<>();

        FilterChain chain = (req, res) ->
                captured.set(
                        RequestMetadataContext.current()
                                .orElseThrow()
                );

        filter.doFilter(
                request,
                response,
                chain
        );

        assertThat(captured.get().requestId())
                .isEqualTo("req-001");

        assertThat(captured.get().traceparent())
                .startsWith(
                        "00-4bf92f3577b34da6a3ce929d0e0e4736-"
                );

        assertThat(captured.get().traceparent())
                .isNotEqualTo(
                        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
                );

        assertThat(captured.get().tracestate())
                .isEqualTo("vendor=value");

        assertThat(RequestMetadataContext.current())
                .isEmpty();
    }

    @Test
    void shouldCreateTraceWhenHeaderIsMissing()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<RequestMetadata> captured =
                new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (req, res) ->
                        captured.set(
                                RequestMetadataContext.current()
                                        .orElseThrow()
                        )
        );

        assertThat(captured.get().traceparent())
                .startsWith("00-");

        assertThat(response.getHeader("traceparent"))
                .isEqualTo(captured.get().traceparent());
    }
}