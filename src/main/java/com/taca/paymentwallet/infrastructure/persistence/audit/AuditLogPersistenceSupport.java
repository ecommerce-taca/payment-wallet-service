package com.taca.paymentwallet.infrastructure.persistence.audit;

import com.taca.paymentwallet.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public class AuditLogPersistenceSupport {

    private final AuditLogJpaRepository repository;
    private final PersistenceUuidGenerator uuidGenerator;
    private final ObjectMapper objectMapper;

    public AuditLogPersistenceSupport(
            AuditLogJpaRepository repository,
            PersistenceUuidGenerator uuidGenerator,
            ObjectMapper objectMapper
    ) {
        this.repository =
                Objects.requireNonNull(repository);

        this.uuidGenerator =
                Objects.requireNonNull(uuidGenerator);

        this.objectMapper =
                Objects.requireNonNull(objectMapper);
    }

    public void append(
            AuditLogRecord record
    ) {
        Objects.requireNonNull(
                record,
                "record must not be null"
        );

        validateMetadata(
                record.metadata()
        );

        AuditLogJpaEntity entity =
                new AuditLogJpaEntity();

        /*
         * Audit log không có Domain ID.
         * ID là persistence-only UUIDv7.
         */
        entity.setId(
                uuidGenerator.next(
                        record.occurredAt()
                )
        );

        entity.setActorUserId(
                record.actorUserId()
        );

        entity.setActorType(
                record.actorType().name()
        );

        entity.setAction(
                record.action()
        );

        entity.setTargetType(
                record.targetType()
        );

        entity.setTargetId(
                record.targetId()
        );

        entity.setReason(
                record.reason()
        );

        entity.setMetadata(
                record.metadata()
        );

        entity.setOccurredAt(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                record.occurredAt()
                        )
        );

        repository.save(entity);
    }

    private void validateMetadata(
            String metadata
    ) {
        if (metadata == null) {
            return;
        }

        try {
            objectMapper.readTree(
                    metadata
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "audit metadata must be valid JSON",
                    exception
            );
        }
    }
}