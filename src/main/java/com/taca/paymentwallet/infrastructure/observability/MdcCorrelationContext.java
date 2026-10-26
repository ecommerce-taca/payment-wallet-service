package com.taca.paymentwallet.infrastructure.observability;

import com.taca.paymentwallet.application.metadata.RequestMetadata;
import org.slf4j.MDC;

public final class MdcCorrelationContext {

    static final String REQUEST_ID = "request_id";
    static final String TRACE_ID = "trace_id";
    static final String SPAN_ID = "span_id";

    private MdcCorrelationContext() {
    }

    public static void bind(RequestMetadata metadata) {
        clear();

        if (metadata == null) {
            return;
        }

        put(REQUEST_ID, metadata.requestId());

        String traceparent = metadata.traceparent();

        if (traceparent == null || traceparent.length() != 55) {
            return;
        }

        put(
                TRACE_ID,
                traceparent.substring(3, 35)
        );

        put(
                SPAN_ID,
                traceparent.substring(36, 52)
        );
    }

    public static void clear() {
        MDC.remove(REQUEST_ID);
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
    }

    private static void put(
            String key,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}