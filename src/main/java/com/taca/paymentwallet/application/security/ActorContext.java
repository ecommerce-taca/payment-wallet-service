package com.taca.paymentwallet.application.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ActorContext(
        UUID userId,
        Set<String> roles,
        Set<String> permissions,
        Set<UUID> shopScope
) {

    public ActorContext {
        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        roles = normalizeValues(roles, "roles");

        permissions = normalizeValues(permissions, "permissions");

        shopScope = immutableCopy(shopScope, "shopScope");
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        return roles.contains(
                role.trim()
        );
    }

    public boolean hasPermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }

        return permissions.contains(
                permission.trim()
        );
    }

    public boolean hasShopAccess(UUID shopId) {
        return shopId != null && shopScope.contains(shopId);
    }

    private static Set<String> normalizeValues(
            Set<String> values,
            String fieldName
    ) {
        Objects.requireNonNull(
                values, fieldName + " must not be null"
        );

        Set<String> normalized = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        fieldName + " must not contain blank values"
                );
            }

            normalized.add(value.trim());
        }

        return Collections.unmodifiableSet(
                normalized
        );
    }

    private static <T> Set<T> immutableCopy(
            Set<T> values,
            String fieldName
    ) {
        Objects.requireNonNull(
                values, fieldName + " must not be null"
        );

        Set<T> copy = new LinkedHashSet<>();

        for (T value : values) {
            if (value == null) {
                throw new IllegalArgumentException(
                        fieldName + " must not contain null values"
                );
            }

            copy.add(value);
        }

        return Collections.unmodifiableSet(
                copy
        );
    }
}