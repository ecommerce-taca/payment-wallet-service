package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountJpaRepository extends JpaRepository<LedgerAccountJpaEntity, UUID> {

    Optional<LedgerAccountJpaEntity> findByAccountCodeAndCurrency(String accountCode, String currency);

    Optional<LedgerAccountJpaEntity> findByOwnerTypeAndOwnerIdAndAccountTypeAndCurrency(
            String ownerType,
            UUID ownerId,
            String accountType,
            String currency
    );
}