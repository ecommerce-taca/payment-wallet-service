package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PayoutJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PayoutJpaRepository extends JpaRepository<PayoutJpaEntity, UUID> {

    Optional<PayoutJpaEntity> findByShopIdAndIdempotencyKey(
            UUID shopId,
            String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from PayoutJpaEntity p
            where p.id = :id
            """)
    Optional<PayoutJpaEntity> findByIdForUpdate(
            @Param("id") UUID id
    );
}