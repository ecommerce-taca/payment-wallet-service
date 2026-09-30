package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.PayoutRepositoryPort;
import com.taca.paymentwallet.domain.payout.Payout;
import com.taca.paymentwallet.domain.payout.PayoutStatus;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PayoutJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PayoutPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PayoutJpaRepository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class PayoutRepositoryAdapter
        implements PayoutRepositoryPort {

    private final PayoutJpaRepository repository;
    private final PayoutPersistenceMapper mapper;
    private final ClockPort clockPort;

    public PayoutRepositoryAdapter(
            PayoutJpaRepository repository,
            PayoutPersistenceMapper mapper,
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
    public Optional<Payout> findById(
            PayoutId payoutId
    ) {
        Objects.requireNonNull(
                payoutId,
                "payoutId must not be null"
        );

        return repository
                .findById(payoutId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Payout> findByIdForUpdate(
            PayoutId payoutId
    ) {
        Objects.requireNonNull(
                payoutId,
                "payoutId must not be null"
        );

        return repository
                .findByIdForUpdate(
                        payoutId.value()
                )
                .map(mapper::toDomain);
    }

    @Override
    public Payout save(
            Payout payout
    ) {
        Objects.requireNonNull(
                payout,
                "payout must not be null"
        );

        Optional<PayoutJpaEntity> existing =
                repository.findById(
                        payout.id().value()
                );

        if (existing.isPresent()) {
            return updateExisting(
                    payout,
                    existing.get()
            );
        }

        return insertNew(payout);
    }

    private Payout insertNew(
            Payout payout
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        PayoutJpaEntity entity =
                mapper.toNewEntity(
                        payout,
                        now
                );

        updateCompletionMetadata(
                payout,
                entity,
                now
        );

        repository.save(entity);

        return payout;
    }

    private Payout updateExisting(
            Payout payout,
            PayoutJpaEntity entity
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        mapper.updateEntity(
                payout,
                entity,
                now
        );

        updateCompletionMetadata(
                payout,
                entity,
                now
        );

        repository.save(entity);

        return payout;
    }

    private void updateCompletionMetadata(
            Payout payout,
            PayoutJpaEntity entity,
            LocalDateTime now
    ) {
        if (entity.getCompletedAt() != null) {
            return;
        }

        if (isTerminal(payout.status())) {
            entity.setCompletedAt(now);
        }
    }

    private boolean isTerminal(
            PayoutStatus status
    ) {
        return status == PayoutStatus.SUCCESS
                || status == PayoutStatus.FAILED
                || status == PayoutStatus.CANCELLED;
    }

    private LocalDateTime currentPersistenceTime() {
        return PersistenceTimeMapper
                .toLocalDateTime(
                        clockPort.now()
                );
    }
}