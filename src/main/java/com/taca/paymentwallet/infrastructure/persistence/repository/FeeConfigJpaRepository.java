package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface FeeConfigJpaRepository extends JpaRepository<FeeConfigJpaEntity, UUID> {

    Optional<FeeConfigJpaEntity> findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            String scope,
            LocalDateTime effectiveAt
    );
}