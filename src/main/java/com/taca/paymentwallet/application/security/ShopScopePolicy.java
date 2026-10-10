package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;

import java.util.Objects;
import java.util.UUID;

public final class ShopScopePolicy {

    private final AuthenticationGuard authenticationGuard;

    public ShopScopePolicy(AuthenticationGuard authenticationGuard) {
        this.authenticationGuard = Objects.requireNonNull(authenticationGuard);
    }

    public ActorContext requireShopAccess(UUID shopId) {
        Objects.requireNonNull(shopId, "shopId must not be null");

        ActorContext actor = authenticationGuard.requireActor();

        if (!actor.hasShopAccess(shopId)) {
            throw new ForbiddenException();
        }

        return actor;
    }
}