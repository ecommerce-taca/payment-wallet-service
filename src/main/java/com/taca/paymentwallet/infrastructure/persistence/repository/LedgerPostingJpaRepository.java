package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerPostingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerPostingJpaRepository extends JpaRepository<LedgerPostingJpaEntity, UUID> {

    Optional<LedgerPostingJpaEntity> findByBusinessKey(String businessKey);

    boolean existsByBusinessKey(String businessKey);
}