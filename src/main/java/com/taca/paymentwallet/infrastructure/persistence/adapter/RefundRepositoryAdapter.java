package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.RefundRepositoryPort;
import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.refund.RefundStatus;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.infrastructure.persistence.entity.RefundJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.RefundPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.RefundJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RefundRepositoryAdapter
        implements RefundRepositoryPort {

    private static final List<String> PENDING_STATUSES =
            List.of(
                    RefundStatus.REQUESTED.name(),
                    RefundStatus.PROCESSING.name()
            );

    private final RefundJpaRepository repository;
    private final RefundPersistenceMapper mapper;
    private final ClockPort clockPort;

    public RefundRepositoryAdapter(
            RefundJpaRepository repository,
            RefundPersistenceMapper mapper,
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
    public Optional<Refund> findById(
            RefundId refundId
    ) {
        Objects.requireNonNull(
                refundId,
                "refundId must not be null"
        );

        return repository
                .findById(refundId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Refund> findByIdForUpdate(
            RefundId refundId
    ) {
        Objects.requireNonNull(
                refundId,
                "refundId must not be null"
        );

        return repository
                .findByIdForUpdate(
                        refundId.value()
                )
                .map(mapper::toDomain);
    }

    @Override
    public Money sumPendingRefundAmount(
            PaymentId paymentId
    ) {
        Objects.requireNonNull(
                paymentId,
                "paymentId must not be null"
        );

        Long amount =
                repository.sumAmountByPaymentIdAndStatuses(
                        paymentId.value(),
                        PENDING_STATUSES
                );

        return Money.vnd(
                amount == null ? 0L : amount
        );
    }

    @Override
    public Refund save(
            Refund refund
    ) {
        Objects.requireNonNull(
                refund,
                "refund must not be null"
        );

        Optional<RefundJpaEntity> existing =
                repository.findById(
                        refund.id().value()
                );

        if (existing.isPresent()) {
            return updateExisting(
                    refund,
                    existing.get()
            );
        }

        return insertNew(refund);
    }

    private Refund insertNew(
            Refund refund
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        RefundJpaEntity entity =
                mapper.toNewEntity(
                        refund,
                        now
                );

        updateCompletionMetadata(
                refund,
                entity,
                now
        );

        repository.save(entity);

        return refund;
    }

    private Refund updateExisting(
            Refund refund,
            RefundJpaEntity entity
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        mapper.updateEntity(
                refund,
                entity,
                now
        );

        updateCompletionMetadata(
                refund,
                entity,
                now
        );

        repository.save(entity);

        return refund;
    }

    private void updateCompletionMetadata(
            Refund refund,
            RefundJpaEntity entity,
            LocalDateTime now
    ) {
        if (entity.getCompletedAt() != null) {
            return;
        }

        if (isTerminal(refund.status())) {
            entity.setCompletedAt(now);
        }
    }

    private boolean isTerminal(
            RefundStatus status
    ) {
        return status == RefundStatus.SUCCESS
                || status == RefundStatus.FAILED
                || status == RefundStatus.CANCELLED;
    }

    private LocalDateTime currentPersistenceTime() {
        return PersistenceTimeMapper
                .toLocalDateTime(
                        clockPort.now()
                );
    }
}