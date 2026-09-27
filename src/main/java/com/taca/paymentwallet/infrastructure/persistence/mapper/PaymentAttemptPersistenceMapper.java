package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.payment.PaymentAttempt;
import com.taca.paymentwallet.domain.payment.PaymentAttemptStatus;
import com.taca.paymentwallet.domain.valueobject.PaymentAttemptId;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAttemptJpaEntity;

import java.time.LocalDateTime;

public class PaymentAttemptPersistenceMapper {

    public PaymentAttemptJpaEntity toNewEntity(
            PaymentAttempt attempt,
            LocalDateTime createdAt
    ) {
        PaymentAttemptJpaEntity entity =
                new PaymentAttemptJpaEntity();

        entity.setId(
                attempt.id().value()
        );

        entity.setPaymentId(
                attempt.paymentId().value()
        );

        entity.setProvider(
                attempt.provider()
        );

        entity.setProviderTransactionRef(
                attempt.providerTransactionRef()
        );

        entity.setStatus(
                attempt.status().name()
        );

        entity.setRequestHash(
                attempt.requestHash()
        );

        entity.setPaymentUrlHash(
                attempt.paymentUrlHash()
        );

        entity.setExpiresAt(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                attempt.expiresAt()
                        )
        );

        entity.setCompletedAt(
                attempt.completedAt() == null
                        ? null
                        : PersistenceTimeMapper
                        .toLocalDateTime(
                                attempt.completedAt()
                        )
        );

        entity.setFailureCode(attempt.failureCode());

        entity.setCreatedAt(createdAt);

        entity.setUpdatedAt(createdAt);

        return entity;
    }

    public void updateEntity(
            PaymentAttempt attempt,
            PaymentAttemptJpaEntity entity,
            LocalDateTime updatedAt
    ) {
        entity.setStatus(
                attempt.status().name()
        );

        entity.setCompletedAt(
                attempt.completedAt() == null
                        ? null
                        : PersistenceTimeMapper
                        .toLocalDateTime(
                                attempt.completedAt()
                        )
        );

        entity.setFailureCode(attempt.failureCode());

        entity.setUpdatedAt(updatedAt);
    }

    public PaymentAttempt toDomain(
            PaymentAttemptJpaEntity entity
    ) {
        return PaymentAttempt.rehydrate(
                new PaymentAttemptId(
                        entity.getId()
                ),
                new PaymentId(
                        entity.getPaymentId()
                ),
                entity.getProvider(),
                entity.getProviderTransactionRef(),
                PaymentAttemptStatus.valueOf(
                        entity.getStatus()
                ),
                entity.getRequestHash(),
                entity.getPaymentUrlHash(),
                PersistenceTimeMapper.toInstant(
                        entity.getExpiresAt()
                ),
                entity.getCompletedAt() == null
                        ? null
                        : PersistenceTimeMapper
                        .toInstant(
                                entity.getCompletedAt()
                        ),
                entity.getFailureCode()
        );
    }
}