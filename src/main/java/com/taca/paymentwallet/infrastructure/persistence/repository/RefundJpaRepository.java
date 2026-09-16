package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.RefundJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefundJpaRepository extends JpaRepository<RefundJpaEntity, UUID> {

    Optional<RefundJpaEntity> findByPaymentIdAndIdempotencyKey(
            UUID paymentId,
            String idempotencyKey
    );
}
