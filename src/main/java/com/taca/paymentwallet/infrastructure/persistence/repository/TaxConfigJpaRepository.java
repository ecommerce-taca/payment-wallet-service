package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TaxConfigJpaRepository extends JpaRepository<TaxConfigJpaEntity, UUID> {

    Optional<TaxConfigJpaEntity> findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            String scope,
            LocalDateTime effectiveAt
    );
}