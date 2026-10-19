package com.taca.paymentwallet.application.command;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CreateAdminTaxConfigCommand(
        String scope,
        UUID categoryId,
        int rateBps,
        Instant effectiveFrom,
        String note,
        UUID actorUserId
) {

    private static final Set<String> SCOPES = Set.of("PLATFORM", "CATEGORY");

    public CreateAdminTaxConfigCommand {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope must not be blank");
        }

        scope = scope.trim().toUpperCase();

        if (!SCOPES.contains(scope)) {
            throw new IllegalArgumentException("scope must be PLATFORM or CATEGORY");
        }

        if ("PLATFORM".equals(scope) && categoryId != null) {
            throw new IllegalArgumentException("PLATFORM scope must not have categoryId");
        }

        if ("CATEGORY".equals(scope) && categoryId == null) {
            throw new IllegalArgumentException("CATEGORY scope requires categoryId");
        }

        if (rateBps < 0 || rateBps > 10_000) {
            throw new IllegalArgumentException("rateBps must be between 0 and 10000");
        }

        if (effectiveFrom == null) {
            throw new IllegalArgumentException("effectiveFrom must not be null");
        }

        if (actorUserId == null) {
            throw new IllegalArgumentException("actorUserId must not be null");
        }

        if (note != null) {
            note = note.trim();

            if (note.isEmpty()) {
                note = null;
            }

            if (note != null && note.length() > 500) {
                throw new IllegalArgumentException("note must not exceed 500 characters");
            }
        }
    }
}