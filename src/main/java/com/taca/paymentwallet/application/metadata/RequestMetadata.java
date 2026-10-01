package com.taca.paymentwallet.application.metadata;

public record RequestMetadata(
        String requestId,
        String traceparent,
        String tracestate
) {

    public boolean isEmpty() {
        return isBlank(requestId)
                && isBlank(traceparent)
                && isBlank(tracestate);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}