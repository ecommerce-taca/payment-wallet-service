package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAttemptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptJpaEntity, UUID> {

    List<PaymentAttemptJpaEntity> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    Optional<PaymentAttemptJpaEntity> findByProviderAndProviderTransactionRef(
            String provider,
            String providerTransactionRef
    );

    boolean existsByPaymentIdAndStatus(UUID paymentId, String status);
}