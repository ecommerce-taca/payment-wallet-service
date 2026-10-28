package com.taca.paymentwallet.infrastructure.observability;

import com.taca.paymentwallet.application.metadata.RequestMetadata;

/** @deprecated Use the application-layer correlation context. */
@Deprecated(forRemoval = false)
public final class MdcCorrelationContext {

    private MdcCorrelationContext() {
    }

    public static void bind(RequestMetadata metadata) {
        com.taca.paymentwallet.application.metadata.MdcCorrelationContext.bind(metadata);
    }

    public static void clear() {
        com.taca.paymentwallet.application.metadata.MdcCorrelationContext.clear();
    }
}
