package com.taca.paymentwallet.application.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Objects;

public class HttpRequestObservation {

    public static final String REQUEST_TOTAL =
            "payment_wallet_http_request_total";

    public static final String REQUEST_LATENCY =
            "payment_wallet_http_request_latency";

    private final MeterRegistry meterRegistry;

    public HttpRequestObservation(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    public void record(
            String method,
            String route,
            int status,
            Duration duration
    ) {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(route, "route must not be null");
        Objects.requireNonNull(duration, "duration must not be null");

        String statusTag = Integer.toString(status);

        meterRegistry.counter(
                REQUEST_TOTAL,
                "method", method,
                "route", route,
                "status", statusTag
        ).increment();

        Timer.builder(REQUEST_LATENCY)
                .tag("method", method)
                .tag("route", route)
                .tag("status", statusTag)
                .register(meterRegistry)
                .record(duration);
    }
}
