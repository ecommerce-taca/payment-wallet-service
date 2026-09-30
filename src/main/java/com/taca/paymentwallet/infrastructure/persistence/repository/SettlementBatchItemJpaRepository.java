package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementBatchItemJpaRepository extends JpaRepository<SettlementBatchItemJpaEntity, UUID> {

    List<SettlementBatchItemJpaEntity> findByBatchId(UUID batchId);
}
