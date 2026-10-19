package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminAuditPort;
import com.taca.paymentwallet.infrastructure.persistence.audit.AuditActorType;
import com.taca.paymentwallet.infrastructure.persistence.audit.AuditLogPersistenceSupport;
import com.taca.paymentwallet.infrastructure.persistence.audit.AuditLogRecord;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AdminAuditAdapter implements AdminAuditPort {

    private final AuditLogPersistenceSupport support;

    public AdminAuditAdapter(AuditLogPersistenceSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    @Override
    public void append(
            UUID actorUserId,
            String action,
            String targetType,
            UUID targetId,
            String reason,
            Instant occurredAt
    ) {
        support.append(
                new AuditLogRecord(
                        actorUserId,
                        AuditActorType.ADMIN,
                        action,
                        targetType,
                        targetId,
                        reason,
                        null,
                        occurredAt
                )
        );
    }
}