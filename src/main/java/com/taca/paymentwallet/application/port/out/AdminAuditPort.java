package com.taca.paymentwallet.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface AdminAuditPort {

    void append(
            UUID actorUserId,
            String action,
            String targetType,
            UUID targetId,
            String reason,
            Instant occurredAt
    );
}