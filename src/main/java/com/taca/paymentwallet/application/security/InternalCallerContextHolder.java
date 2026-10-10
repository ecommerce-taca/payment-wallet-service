package com.taca.paymentwallet.application.security;

import java.util.Optional;

public final class InternalCallerContextHolder {

    private static final ThreadLocal<InternalCallerContext> CONTEXT = new ThreadLocal<>();

    private InternalCallerContextHolder() {
    }

    public static void set(InternalCallerContext context) {
        if (context == null) {
            CONTEXT.remove();
            return;
        }

        CONTEXT.set(context);
    }

    public static Optional<InternalCallerContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}