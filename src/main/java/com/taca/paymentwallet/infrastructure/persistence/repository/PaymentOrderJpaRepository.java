package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentOrderJpaRepository extends JpaRepository<PaymentOrderJpaEntity, UUID> {

    List<PaymentOrderJpaEntity> findByPaymentId(UUID paymentId);

    List<PaymentOrderJpaEntity> findByOrderId(UUID orderId);
}