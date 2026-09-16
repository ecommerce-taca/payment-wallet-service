package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    List<AuditLogJpaEntity> findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
            String targetType,
            UUID targetId
    );
}