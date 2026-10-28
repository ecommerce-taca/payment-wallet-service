package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.infrastructure.observability.HttpRequestObservation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpMetricsFilterTest {

    private static final String REQUEST_TOTAL =
            "payment_wallet_http_request_total";

    private final SimpleMeterRegistry registry =
            new SimpleMeterRegistry();

    private final HttpMetricsFilter filter =
            new HttpMetricsFilter(
                    new HttpRequestObservation(registry)
            );

    @Test
    void shouldRecordResolvedRouteTemplate() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/v1/payments/payment-123"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            req.setAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                    "/api/v1/payments/{paymentId}"
            );

            ((MockHttpServletResponse) res).setStatus(200);
        };

        filter.doFilter(request, response, chain);

        assertThat(
                registry.get(REQUEST_TOTAL)
                        .tags(
                                "method", "GET",
                                "route", "/api/v1/payments/{paymentId}",
                                "status", "200"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void shouldUseUnmatchedForUnknownRoute() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/unknown"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) ->
                        ((MockHttpServletResponse) res).setStatus(404)
        );

        assertThat(
                registry.get(REQUEST_TOTAL)
                        .tags(
                                "method", "GET",
                                "route", "unmatched",
                                "status", "404"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }

    @Test
    void shouldRecordEscapedFailureAs500() {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/api/v1/payments"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        assertThatThrownBy(() ->
                filter.doFilter(
                        request,
                        response,
                        (req, res) -> {
                            req.setAttribute(
                                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                                    "/api/v1/payments"
                            );

                            throw new ServletException("boom");
                        }
                )
        ).isInstanceOf(ServletException.class);

        assertThat(
                registry.get(REQUEST_TOTAL)
                        .tags(
                                "method", "POST",
                                "route", "/api/v1/payments",
                                "status", "500"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }
}
