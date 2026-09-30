package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.wallet.LedgerAccount;
import com.taca.paymentwallet.domain.wallet.LedgerAccountType;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerAccountJpaEntity;

import java.util.Objects;

public class LedgerAccountPersistenceMapper {

    public LedgerAccount toDomain(
            LedgerAccountJpaEntity entity
    ) {
        Objects.requireNonNull(
                entity,
                "entity must not be null"
        );

        ShopId ownerId = entity.getOwnerId() == null
                ? null
                : new ShopId(entity.getOwnerId());

        return new LedgerAccount(
                new LedgerAccountId(entity.getId()),
                entity.getAccountCode(),
                LedgerAccountType.valueOf(
                        entity.getAccountType()
                ),
                entity.getOwnerType(),
                ownerId,
                entity.getCurrency()
        );
    }
}