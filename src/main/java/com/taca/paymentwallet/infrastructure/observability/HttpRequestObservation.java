package com.taca.paymentwallet.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;

/** @deprecated Use the application-layer observation type. */
@Deprecated(forRemoval = false)
public class HttpRequestObservation extends com.taca.paymentwallet.application.observability.HttpRequestObservation {

    public HttpRequestObservation(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }
}
