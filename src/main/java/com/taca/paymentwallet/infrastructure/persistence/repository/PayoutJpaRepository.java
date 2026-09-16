package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PayoutJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayoutJpaRepository extends JpaRepository<PayoutJpaEntity, UUID> {

    Optional<PayoutJpaEntity> findByShopIdAndIdempotencyKey(
            UUID shopId,
            String idempotencyKey
    );
}