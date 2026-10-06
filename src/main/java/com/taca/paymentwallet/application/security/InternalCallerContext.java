package com.taca.paymentwallet.application.security;

import java.util.Objects;

public record InternalCallerContext(InternalService service) {

    public InternalCallerContext {
        Objects.requireNonNull(service, "service must not be null");
    }
}