package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.exception.IdempotencyRecordNotFoundException;
import com.taca.paymentwallet.application.idempotency.IdempotencyRecord;
import com.taca.paymentwallet.application.idempotency.IdempotencyScope;
import com.taca.paymentwallet.application.idempotency.IdempotencyStatus;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.IdempotencyPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class IdempotencyPersistenceAdapter
        implements IdempotencyPort {

    private final IdempotencyKeyJpaRepository repository;
    private final ClockPort clockPort;
    private final PersistenceUuidGenerator uuidGenerator;
    private final Duration ttl;

    public IdempotencyPersistenceAdapter(
            IdempotencyKeyJpaRepository repository,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator,
            Duration ttl
    ) {
        this.repository =
                Objects.requireNonNull(repository);

        this.clockPort =
                Objects.requireNonNull(clockPort);

        this.uuidGenerator =
                Objects.requireNonNull(uuidGenerator);

        this.ttl =
                Objects.requireNonNull(ttl);

        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "idempotency ttl must be positive"
            );
        }
    }

    @Override
    public Optional<IdempotencyRecord> find(
            IdempotencyScope scope,
            String idempotencyKey
    ) {
        Objects.requireNonNull(
                scope,
                "scope must not be null"
        );

        String normalizedKey =
                normalizeKey(idempotencyKey);

        ScopeParts scopeParts =
                parseScope(scope);

        Instant now =
                clockPort.now();

        return repository
                .findByScopeAndScopeIdAndIdempotencyKey(
                        scopeParts.scopeType(),
                        scopeParts.scopeId(),
                        normalizedKey
                )
                .filter(entity ->
                        !isExpired(
                                entity,
                                now
                        )
                )
                .map(entity ->
                        toRecord(
                                scope,
                                entity
                        )
                );
    }

    @Override
    public IdempotencyRecord reserve(
            IdempotencyScope scope,
            String idempotencyKey,
            String requestHash
    ) {
        Objects.requireNonNull(
                scope,
                "scope must not be null"
        );

        String normalizedKey =
                normalizeKey(idempotencyKey);

        String normalizedHash =
                normalizeRequestHash(requestHash);

        ScopeParts scopeParts =
                parseScope(scope);

        Instant now =
                clockPort.now();

        LocalDateTime nowPersistence =
                PersistenceTimeMapper
                        .toLocalDateTime(now);

        Optional<IdempotencyKeyJpaEntity> existing =
                repository
                        .findByScopeAndScopeIdAndIdempotencyKey(
                                scopeParts.scopeType(),
                                scopeParts.scopeId(),
                                normalizedKey
                        );

        IdempotencyKeyJpaEntity entity;

        if (existing.isPresent()) {
            entity =
                    reuseExpiredRecord(
                            existing.get(),
                            normalizedHash,
                            now,
                            nowPersistence
                    );
        } else {
            entity =
                    createNewRecord(
                            scopeParts,
                            normalizedKey,
                            normalizedHash,
                            now,
                            nowPersistence
                    );
        }

        repository.save(entity);

        return toRecord(
                scope,
                entity
        );
    }

    @Override
    public void markSucceeded(
            IdempotencyScope scope,
            String idempotencyKey,
            String responsePayload
    ) {
        Objects.requireNonNull(
                scope,
                "scope must not be null"
        );

        if (responsePayload == null
                || responsePayload.isBlank()) {
            throw new IllegalArgumentException(
                    "responsePayload must not be blank"
            );
        }

        IdempotencyKeyJpaEntity entity =
                findRequiredEntity(
                        scope,
                        idempotencyKey
                );

        entity.setStatus(
                IdempotencyStatus.SUCCEEDED.name()
        );

        entity.setResponseSnapshot(
                responsePayload
        );

        entity.setFailureCode(null);

        entity.setUpdatedAt(
                currentPersistenceTime()
        );

        repository.save(entity);
    }

    @Override
    public void markFailed(
            IdempotencyScope scope,
            String idempotencyKey,
            String failureCode
    ) {
        Objects.requireNonNull(
                scope,
                "scope must not be null"
        );

        if (failureCode == null
                || failureCode.isBlank()) {
            throw new IllegalArgumentException(
                    "failureCode must not be blank"
            );
        }

        IdempotencyKeyJpaEntity entity =
                findRequiredEntity(
                        scope,
                        idempotencyKey
                );

        entity.setStatus(
                IdempotencyStatus.FAILED.name()
        );

        entity.setResponseSnapshot(null);

        entity.setFailureCode(
                failureCode.trim()
        );

        entity.setUpdatedAt(
                currentPersistenceTime()
        );

        repository.save(entity);
    }

    private IdempotencyKeyJpaEntity createNewRecord(
            ScopeParts scopeParts,
            String idempotencyKey,
            String requestHash,
            Instant now,
            LocalDateTime nowPersistence
    ) {
        IdempotencyKeyJpaEntity entity =
                new IdempotencyKeyJpaEntity();

        entity.setId(
                uuidGenerator.next(now)
        );

        entity.setScope(
                scopeParts.scopeType()
        );

        entity.setScopeId(
                scopeParts.scopeId()
        );

        entity.setIdempotencyKey(
                idempotencyKey
        );

        entity.setRequestHash(
                requestHash
        );

        entity.setStatus(
                IdempotencyStatus.PROCESSING.name()
        );

        entity.setResponseSnapshot(null);
        entity.setFailureCode(null);

        entity.setCreatedAt(
                nowPersistence
        );

        entity.setUpdatedAt(
                nowPersistence
        );

        entity.setExpiresAt(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                now.plus(ttl)
                        )
        );

        return entity;
    }

    private IdempotencyKeyJpaEntity reuseExpiredRecord(
            IdempotencyKeyJpaEntity entity,
            String requestHash,
            Instant now,
            LocalDateTime nowPersistence
    ) {
        if (!isExpired(entity, now)) {
            throw new IllegalStateException(
                    "active idempotency reservation already exists"
            );
        }

        entity.setRequestHash(
                requestHash
        );

        entity.setStatus(
                IdempotencyStatus.PROCESSING.name()
        );

        entity.setResponseSnapshot(null);
        entity.setFailureCode(null);

        entity.setCreatedAt(
                nowPersistence
        );

        entity.setUpdatedAt(
                nowPersistence
        );

        entity.setExpiresAt(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                now.plus(ttl)
                        )
        );

        return entity;
    }

    private IdempotencyKeyJpaEntity findRequiredEntity(
            IdempotencyScope scope,
            String idempotencyKey
    ) {
        ScopeParts parts =
                parseScope(scope);

        String normalizedKey =
                normalizeKey(idempotencyKey);

        return repository
                .findByScopeAndScopeIdAndIdempotencyKey(
                        parts.scopeType(),
                        parts.scopeId(),
                        normalizedKey
                )
                .orElseThrow(
                        () ->
                                new IdempotencyRecordNotFoundException(
                                        scope,
                                        normalizedKey
                                )
                );
    }

    private IdempotencyRecord toRecord(
            IdempotencyScope scope,
            IdempotencyKeyJpaEntity entity
    ) {
        return new IdempotencyRecord(
                scope,
                entity.getIdempotencyKey(),
                entity.getRequestHash(),
                IdempotencyStatus.valueOf(
                        entity.getStatus()
                ),
                entity.getResponseSnapshot(),
                entity.getFailureCode(),
                PersistenceTimeMapper.toInstant(
                        entity.getCreatedAt()
                ),
                PersistenceTimeMapper.toInstant(
                        entity.getUpdatedAt()
                )
        );
    }

    private boolean isExpired(
            IdempotencyKeyJpaEntity entity,
            Instant now
    ) {
        Instant expiresAt =
                PersistenceTimeMapper.toInstant(
                        entity.getExpiresAt()
                );

        return !expiresAt.isAfter(now);
    }

    private ScopeParts parseScope(
            IdempotencyScope scope
    ) {
        String value =
                scope.value();

        int separator =
                value.indexOf(':');

        if (separator <= 0
                || separator == value.length() - 1) {
            throw new IllegalArgumentException(
                    "invalid idempotency scope: "
                            + value
            );
        }

        String scopeType =
                value.substring(
                        0,
                        separator
                );

        String scopeId =
                value.substring(
                        separator + 1
                );

        return new ScopeParts(
                scopeType,
                scopeId
        );
    }

    private String normalizeKey(
            String idempotencyKey
    ) {
        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "idempotencyKey must not be blank"
            );
        }

        return idempotencyKey.trim();
    }

    private String normalizeRequestHash(
            String requestHash
    ) {
        if (requestHash == null
                || requestHash.isBlank()) {
            throw new IllegalArgumentException(
                    "requestHash must not be blank"
            );
        }

        return requestHash.trim();
    }

    private LocalDateTime currentPersistenceTime() {
        return PersistenceTimeMapper
                .toLocalDateTime(
                        clockPort.now()
                );
    }

    private record ScopeParts(
            String scopeType,
            String scopeId
    ) {
    }
}