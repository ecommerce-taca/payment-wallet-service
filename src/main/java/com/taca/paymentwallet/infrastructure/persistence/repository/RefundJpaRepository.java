package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.RefundJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RefundJpaRepository extends JpaRepository<RefundJpaEntity, UUID> {

    Optional<RefundJpaEntity> findByPaymentIdAndIdempotencyKey(
            UUID paymentId,
            String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from RefundJpaEntity r
            where r.id = :id
            """)
    Optional<RefundJpaEntity> findByIdForUpdate(
            @Param("id") UUID id
    );

    @Query("""
            select coalesce(sum(r.amount), 0)
            from RefundJpaEntity r
            where r.paymentId = :paymentId
              and r.status in :statuses
            """)
    Long sumAmountByPaymentIdAndStatuses(
            @Param("paymentId") UUID paymentId,
            @Param("statuses") Collection<String> statuses
    );
}