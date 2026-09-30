package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.wallet.LedgerEntry;
import com.taca.paymentwallet.domain.wallet.LedgerEntryType;
import com.taca.paymentwallet.domain.wallet.LedgerPosting;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerPostingJpaEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LedgerPostingPersistenceMapper {

    public LedgerPosting toDomain(
            LedgerPostingJpaEntity postingEntity,
            List<LedgerEntryJpaEntity> entryEntities
    ) {
        Objects.requireNonNull(postingEntity);
        Objects.requireNonNull(entryEntities);

        List<LedgerEntry> entries =
                entryEntities.stream()
                        .map(this::toDomainEntry)
                        .toList();

        return new LedgerPosting(
                new LedgerPostingId(
                        postingEntity.getId()
                ),
                postingEntity.getPostingType(),
                postingEntity.getBusinessKey(),
                postingEntity.getReferenceType(),
                postingEntity
                        .getReferenceId()
                        .toString(),
                entries
        );
    }

    public LedgerPostingJpaEntity toPostingEntity(
            LedgerPosting posting,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(posting);
        Objects.requireNonNull(createdAt);

        LedgerPostingJpaEntity entity =
                new LedgerPostingJpaEntity();

        entity.setId(
                posting.id().value()
        );

        entity.setPostingType(
                posting.postingType()
        );

        entity.setBusinessKey(
                posting.businessKey()
        );

        entity.setReferenceType(
                posting.referenceType()
        );

        entity.setReferenceId(
                UUID.fromString(
                        posting.referenceId()
                )
        );

        entity.setCreatedAt(createdAt);

        return entity;
    }

    public LedgerEntry toDomainEntry(
            LedgerEntryJpaEntity entity
    ) {
        Objects.requireNonNull(entity);

        return new LedgerEntry(
                new LedgerAccountId(
                        entity.getAccountId()
                ),
                LedgerEntryType.valueOf(
                        entity.getEntryType()
                ),
                Money.vnd(
                        entity.getAmount()
                )
        );
    }

    public LedgerEntryJpaEntity toEntryEntity(
            LedgerPostingId postingId,
            LedgerEntry entry,
            UUID rowId,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(postingId);
        Objects.requireNonNull(entry);
        Objects.requireNonNull(rowId);
        Objects.requireNonNull(createdAt);

        LedgerEntryJpaEntity entity =
                new LedgerEntryJpaEntity();

        entity.setId(rowId);

        entity.setPostingId(
                postingId.value()
        );

        entity.setAccountId(
                entry.accountId().value()
        );

        entity.setEntryType(
                entry.entryType().name()
        );

        entity.setAmount(
                entry.amount().amount()
        );

        entity.setCreatedAt(createdAt);

        return entity;
    }
}