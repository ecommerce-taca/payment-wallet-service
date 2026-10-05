package com.taca.paymentwallet.infrastructure.messaging.kafka;

public final class KafkaHeaderNames {

    public static final String EVENT_ID = "event_id";
    public static final String REQUEST_ID = "request_id";
    public static final String TRACEPARENT = "traceparent";
    public static final String TRACESTATE = "tracestate";

    private KafkaHeaderNames() {
    }
}