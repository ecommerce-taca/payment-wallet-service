package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;

import java.util.Objects;

public final class InternalCallerPolicy {

    private final AuthenticationGuard authenticationGuard;

    public InternalCallerPolicy(AuthenticationGuard authenticationGuard) {
        this.authenticationGuard = Objects.requireNonNull(authenticationGuard);
    }

    public void requireOrderCommerce() {
        if (isOrderCommerce()) {
            return;
        }

        authenticationGuard.requireActor();
        throw new ForbiddenException();
    }

    public void requireOrderCommerceOrFinanceOps() {
        if (isOrderCommerce()) {
            return;
        }

        ActorContext actor = authenticationGuard.requireActor();

        if (!actor.hasRole(SecurityRoles.FINANCE_OPS)) {
            throw new ForbiddenException();
        }
    }

    private boolean isOrderCommerce() {
        return InternalCallerContextHolder.current()
                .map(InternalCallerContext::service)
                .filter(InternalService.ORDER_COMMERCE::equals)
                .isPresent();
    }
}