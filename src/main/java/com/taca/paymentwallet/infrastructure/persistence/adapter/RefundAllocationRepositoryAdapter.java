package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.RefundAllocationRepositoryPort;
import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.infrastructure.persistence.entity.RefundAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.RefundAllocationPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.RefundAllocationJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class RefundAllocationRepositoryAdapter
        implements RefundAllocationRepositoryPort {

    private final RefundAllocationJpaRepository repository;
    private final RefundAllocationPersistenceMapper mapper;
    private final ClockPort clockPort;
    private final PersistenceUuidGenerator uuidGenerator;

    public RefundAllocationRepositoryAdapter(
            RefundAllocationJpaRepository repository,
            RefundAllocationPersistenceMapper mapper,
            ClockPort clockPort,
            PersistenceUuidGenerator uuidGenerator
    ) {
        this.repository =
                Objects.requireNonNull(repository);

        this.mapper =
                Objects.requireNonNull(mapper);

        this.clockPort =
                Objects.requireNonNull(clockPort);

        this.uuidGenerator =
                Objects.requireNonNull(uuidGenerator);
    }

    @Override
    public void saveAll(
            RefundId refundId,
            List<RefundAllocation> refundAllocations
    ) {
        Objects.requireNonNull(
                refundId,
                "refundId must not be null"
        );

        Objects.requireNonNull(
                refundAllocations,
                "refundAllocations must not be null"
        );

        if (refundAllocations.isEmpty()) {
            return;
        }

        Instant now =
                clockPort.now();

        LocalDateTime createdAt =
                PersistenceTimeMapper
                        .toLocalDateTime(now);

        List<RefundAllocationJpaEntity> entities =
                refundAllocations.stream()
                        .map(allocation ->
                                mapper.toEntity(
                                        refundId,
                                        allocation,
                                        uuidGenerator.next(now),
                                        createdAt
                                )
                        )
                        .toList();

        repository.saveAll(entities);
    }
}