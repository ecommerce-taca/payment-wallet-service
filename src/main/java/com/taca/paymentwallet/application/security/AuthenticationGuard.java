package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.UnauthenticatedException;

public final class AuthenticationGuard {

    public ActorContext requireActor() {
        return ActorContextHolder.current()
                .orElseThrow(UnauthenticatedException::new);
    }
}