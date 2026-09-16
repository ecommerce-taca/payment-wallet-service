package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAllocationJpaRepository extends JpaRepository<PaymentAllocationJpaEntity, UUID> {

    List<PaymentAllocationJpaEntity> findByPaymentId(UUID paymentId);

    List<PaymentAllocationJpaEntity> findByShopId(UUID shopId);
}