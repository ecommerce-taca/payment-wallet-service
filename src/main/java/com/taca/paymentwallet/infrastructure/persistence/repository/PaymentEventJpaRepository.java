package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentEventJpaRepository extends JpaRepository<PaymentEventJpaEntity, UUID> {

    Optional<PaymentEventJpaEntity> findByProviderAndProviderEventId(
            String provider,
            String providerEventId
    );

    List<PaymentEventJpaEntity> findByPaymentIdOrderByReceivedAtDesc(UUID paymentId);
}