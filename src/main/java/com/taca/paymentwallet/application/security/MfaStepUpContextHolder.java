package com.taca.paymentwallet.application.security;

import java.util.Optional;

public final class MfaStepUpContextHolder {

    private static final ThreadLocal<MfaStepUpContext> CONTEXT = new ThreadLocal<>();

    private MfaStepUpContextHolder() {
    }

    public static void set(MfaStepUpContext context) {
        if (context == null) {
            CONTEXT.remove();
            return;
        }

        CONTEXT.set(context);
    }

    public static Optional<MfaStepUpContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}