package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.OutboxCleanupPort;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.OutboxEventJpaRepository;

import java.time.Instant;
import java.util.Objects;

public class OutboxCleanupPersistenceAdapter
        implements OutboxCleanupPort {

    private final OutboxEventJpaRepository repository;

    public OutboxCleanupPersistenceAdapter(
            OutboxEventJpaRepository repository
    ) {
        this.repository =
                Objects.requireNonNull(repository);
    }

    @Override
    public int deleteCompletedBefore(
            Instant cutoff,
            int batchSize
    ) {
        Objects.requireNonNull(
                cutoff,
                "cutoff must not be null"
        );

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be positive"
            );
        }

        return repository.deleteCompletedBefore(
                PersistenceTimeMapper.toLocalDateTime(
                        cutoff
                ),
                batchSize
        );
    }
}