package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementLineJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettlementLineJpaRepository extends JpaRepository<SettlementLineJpaEntity, UUID> {

    boolean existsByPaymentAllocationId(UUID paymentAllocationId);
}