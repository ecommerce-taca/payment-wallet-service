package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementBatchJpaRepository extends JpaRepository<SettlementBatchJpaEntity, UUID> {

    List<SettlementBatchJpaEntity> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from SettlementBatchJpaEntity b
            where b.id = :id
            """)
    Optional<SettlementBatchJpaEntity> findByIdForUpdate(
            @Param("id") UUID id
    );
}