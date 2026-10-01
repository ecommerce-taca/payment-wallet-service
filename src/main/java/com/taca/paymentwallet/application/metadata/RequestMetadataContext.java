package com.taca.paymentwallet.application.metadata;

import java.util.Optional;

public final class RequestMetadataContext {

    private static final ThreadLocal<RequestMetadata> CONTEXT =
            new ThreadLocal<>();

    private RequestMetadataContext() {
    }

    public static void set(RequestMetadata metadata) {
        if (metadata == null || metadata.isEmpty()) {
            CONTEXT.remove();
            return;
        }

        CONTEXT.set(metadata);
    }

    public static Optional<RequestMetadata> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}