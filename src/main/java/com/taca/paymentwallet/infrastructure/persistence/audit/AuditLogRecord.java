package com.taca.paymentwallet.infrastructure.persistence.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogRecord(
        UUID actorUserId,
        AuditActorType actorType,
        String action,
        String targetType,
        UUID targetId,
        String reason,
        String metadata,
        Instant occurredAt
) {

    public AuditLogRecord {
        if (actorType == null) {
            throw new IllegalArgumentException(
                    "actorType must not be null"
            );
        }

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "action must not be blank"
            );
        }

        if (targetType == null || targetType.isBlank()) {
            throw new IllegalArgumentException(
                    "targetType must not be blank"
            );
        }

        if (targetId == null) {
            throw new IllegalArgumentException(
                    "targetId must not be null"
            );
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "occurredAt must not be null"
            );
        }

        validateActor(
                actorType,
                actorUserId
        );

        action = action.trim();
        targetType = targetType.trim();

        if (reason != null) {
            reason = reason.trim();

            if (reason.isEmpty()) {
                reason = null;
            }
        }

        if (metadata != null) {
            metadata = metadata.trim();

            if (metadata.isEmpty()) {
                metadata = null;
            }
        }
    }

    private static void validateActor(
            AuditActorType actorType,
            UUID actorUserId
    ) {
        if (actorType == AuditActorType.SYSTEM) {
            if (actorUserId != null) {
                throw new IllegalArgumentException(
                        "SYSTEM audit actor must not have actorUserId"
                );
            }

            return;
        }

        if (actorUserId == null) {
            throw new IllegalArgumentException(
                    actorType
                            + " audit actor requires actorUserId"
            );
        }
    }
}