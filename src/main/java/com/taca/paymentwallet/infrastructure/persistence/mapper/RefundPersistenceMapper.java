package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.refund.Refund;
import com.taca.paymentwallet.domain.refund.RefundStatus;
import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.infrastructure.persistence.entity.RefundJpaEntity;

import java.time.LocalDateTime;
import java.util.Objects;

public class RefundPersistenceMapper {

    public Refund toDomain(
            RefundJpaEntity entity
    ) {
        Objects.requireNonNull(
                entity,
                "entity must not be null"
        );

        return Refund.rehydrate(
                new RefundId(entity.getId()),
                new PaymentId(entity.getPaymentId()),
                new Money(
                        entity.getAmount(),
                        entity.getCurrency()
                ),
                entity.getReason(),
                new IdempotencyKey(
                        entity.getIdempotencyKey()
                ),
                RefundStatus.valueOf(
                        entity.getStatus()
                ),
                entity.getFailureCode()
        );
    }

    public RefundJpaEntity toNewEntity(
            Refund refund,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(refund);
        Objects.requireNonNull(createdAt);

        RefundJpaEntity entity =
                new RefundJpaEntity();

        entity.setId(
                refund.id().value()
        );

        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);

        copyDomainState(
                refund,
                entity
        );

        return entity;
    }

    public void updateEntity(
            Refund refund,
            RefundJpaEntity entity,
            LocalDateTime updatedAt
    ) {
        Objects.requireNonNull(refund);
        Objects.requireNonNull(entity);
        Objects.requireNonNull(updatedAt);

        if (!refund.id().value().equals(entity.getId())) {
            throw new IllegalArgumentException(
                    "refund id does not match entity id"
            );
        }

        copyDomainState(
                refund,
                entity
        );

        entity.setUpdatedAt(updatedAt);
    }

    private void copyDomainState(
            Refund refund,
            RefundJpaEntity entity
    ) {
        entity.setPaymentId(
                refund.paymentId().value()
        );

        entity.setAmount(
                refund.amount().amount()
        );

        entity.setCurrency(
                refund.amount().currency()
        );

        entity.setReason(
                refund.reason()
        );

        entity.setStatus(
                refund.status().name()
        );

        entity.setIdempotencyKey(
                refund.idempotencyKey().value()
        );

        entity.setFailureCode(
                refund.failureCode()
        );
    }
}