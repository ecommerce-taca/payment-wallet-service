package com.taca.paymentwallet.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestObservationContractTest {

    @Test
    void shouldKeepAuthoritativeMetricNames() {
        assertThat(HttpRequestObservation.REQUEST_TOTAL)
                .isEqualTo("payment_wallet_http_request_total");

        assertThat(HttpRequestObservation.REQUEST_LATENCY)
                .isEqualTo("payment_wallet_http_request_latency");
    }

    @Test
    void shouldRecordRequestWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HttpRequestObservation observation =
                new HttpRequestObservation(registry);

        observation.record(
                "GET",
                "/api/v1/payments/{paymentId}",
                200,
                Duration.ofMillis(25)
        );

        assertThat(
                registry.get(HttpRequestObservation.REQUEST_TOTAL)
                        .tags(
                                "method", "GET",
                                "route", "/api/v1/payments/{paymentId}",
                                "status", "200"
                        )
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get(HttpRequestObservation.REQUEST_LATENCY)
                        .tags(
                                "method", "GET",
                                "route", "/api/v1/payments/{paymentId}",
                                "status", "200"
                        )
                        .timer()
                        .count()
        ).isEqualTo(1L);
    }
}