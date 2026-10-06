package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.security.ActorContext;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

final class ActorHeaderParser {

    ActorContext parse(
            String userIdHeader,
            String rolesHeader,
            String permissionsHeader,
            String shopScopeHeader
    ) {
        if (isBlank(userIdHeader)) {
            return null;
        }

        UUID userId = parseUuid(userIdHeader.trim(), "X-User-ID");

        return new ActorContext(
                userId,
                parseStrings(rolesHeader),
                parseStrings(permissionsHeader),
                parseUuids(shopScopeHeader)
        );
    }

    private Set<String> parseStrings(String header) {
        if (isBlank(header)) {
            return Set.of();
        }

        Set<String> values = new LinkedHashSet<>();

        for (String value : header.split(",")) {
            String normalized = value.trim();

            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }

        return values;
    }

    private Set<UUID> parseUuids(String header) {
        if (isBlank(header)) {
            return Set.of();
        }

        Set<UUID> values = new LinkedHashSet<>();

        for (String value : header.split(",")) {
            String normalized = value.trim();

            if (!normalized.isEmpty()) {
                values.add(parseUuid(normalized, "X-User-Shop-Scope"));
            }
        }

        return values;
    }

    private UUID parseUuid(String value, String headerName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    headerName + " must contain valid UUID values",
                    exception
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}