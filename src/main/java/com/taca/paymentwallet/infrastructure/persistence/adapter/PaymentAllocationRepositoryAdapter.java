package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.PaymentAllocationRepositoryPort;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PaymentAllocationPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class PaymentAllocationRepositoryAdapter
        implements PaymentAllocationRepositoryPort {

    private final PaymentAllocationJpaRepository repository;
    private final PaymentAllocationPersistenceMapper mapper;
    private final ClockPort clockPort;

    public PaymentAllocationRepositoryAdapter(
            PaymentAllocationJpaRepository repository,
            PaymentAllocationPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        this.repository =
                Objects.requireNonNull(repository);

        this.mapper =
                Objects.requireNonNull(mapper);

        this.clockPort =
                Objects.requireNonNull(clockPort);
    }

    @Override
    public void saveAll(
            List<PaymentAllocation> allocations
    ) {
        Objects.requireNonNull(
                allocations,
                "allocations must not be null"
        );

        if (allocations.isEmpty()) {
            return;
        }

        LocalDateTime createdAt =
                currentPersistenceTime();

        List<PaymentAllocationJpaEntity> entities =
                allocations.stream()
                        .map(allocation ->
                                mapper.toEntity(
                                        allocation,
                                        createdAt
                                )
                        )
                        .toList();

        repository.saveAll(entities);
    }

    @Override
    public List<PaymentAllocation> findByPaymentId(
            PaymentId paymentId
    ) {
        Objects.requireNonNull(
                paymentId,
                "paymentId must not be null"
        );

        return repository
                .findByPaymentIdOrderByCreatedAtAscIdAsc(
                        paymentId.value()
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private LocalDateTime currentPersistenceTime() {
        return PersistenceTimeMapper
                .toLocalDateTime(
                        clockPort.now()
                );
    }
}