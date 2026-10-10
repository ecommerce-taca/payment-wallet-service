package com.taca.paymentwallet.application.security;

import java.util.Optional;

public final class ActorContextHolder {

    private static final ThreadLocal<ActorContext> CONTEXT = new ThreadLocal<>();

    private ActorContextHolder() {
    }

    public static void set(ActorContext actorContext) {
        if (actorContext == null) {
            CONTEXT.remove();
            return;
        }

        CONTEXT.set(actorContext);
    }

    public static Optional<ActorContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}