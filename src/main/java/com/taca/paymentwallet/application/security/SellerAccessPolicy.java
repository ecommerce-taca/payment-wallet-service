package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;

import java.util.Objects;
import java.util.UUID;

public final class SellerAccessPolicy {

    private final AuthenticationGuard authenticationGuard;

    public SellerAccessPolicy(AuthenticationGuard authenticationGuard) {
        this.authenticationGuard = Objects.requireNonNull(authenticationGuard);
    }

    public UUID requireSellerShopId() {
        ActorContext actor = authenticationGuard.requireActor();

        if (!actor.hasRole(SecurityRoles.SELLER) || actor.shopScope().size() != 1) {
            throw new ForbiddenException();
        }

        return actor.shopScope().iterator().next();
    }
}