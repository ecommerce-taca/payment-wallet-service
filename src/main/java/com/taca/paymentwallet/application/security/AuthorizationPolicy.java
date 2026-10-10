package com.taca.paymentwallet.application.security;

import com.taca.paymentwallet.application.exception.ForbiddenException;

import java.util.Objects;

public final class AuthorizationPolicy {

    private final AuthenticationGuard authenticationGuard;

    public AuthorizationPolicy(AuthenticationGuard authenticationGuard) {
        this.authenticationGuard = Objects.requireNonNull(authenticationGuard);
    }

    public ActorContext requireRole(String role) {
        ActorContext actor = authenticationGuard.requireActor();

        if (!actor.hasRole(requireAuthority(role, "role"))) {
            throw new ForbiddenException();
        }

        return actor;
    }

    public ActorContext requirePermission(String permission) {
        ActorContext actor = authenticationGuard.requireActor();

        if (!actor.hasPermission(requireAuthority(permission, "permission"))) {
            throw new ForbiddenException();
        }

        return actor;
    }

    private String requireAuthority(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value.trim();
    }
}