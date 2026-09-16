package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.RefundAllocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefundAllocationJpaRepository extends JpaRepository<RefundAllocationJpaEntity, UUID> {
    
    List<RefundAllocationJpaEntity> findByRefundId(UUID refundId);
}