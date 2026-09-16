package com.taca.paymentwallet.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity extends UuidEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "actor_user_id", columnDefinition = "BINARY(16)")
    private UUID actorUserId;

    @Column(name = "actor_type", nullable = false, length = 40)
    private String actorType;

    @Column(name = "action", nullable = false, length = 120)
    private String action;

    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "target_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Column(name = "reason", length = 500)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}