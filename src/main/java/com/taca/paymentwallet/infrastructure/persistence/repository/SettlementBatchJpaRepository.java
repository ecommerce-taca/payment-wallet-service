package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementBatchJpaRepository extends JpaRepository<SettlementBatchJpaEntity, UUID> {

    List<SettlementBatchJpaEntity> findAllByOrderByCreatedAtDesc();
}