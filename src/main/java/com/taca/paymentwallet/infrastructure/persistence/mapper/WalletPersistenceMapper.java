package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.domain.wallet.Wallet;
import com.taca.paymentwallet.domain.wallet.WalletStatus;
import com.taca.paymentwallet.infrastructure.persistence.entity.WalletJpaEntity;

import java.time.LocalDateTime;
import java.util.Objects;

public class WalletPersistenceMapper {

    public Wallet toDomain(
            WalletJpaEntity entity
    ) {
        Objects.requireNonNull(
                entity,
                "entity must not be null"
        );

        return Wallet.rehydrate(
                new WalletId(entity.getId()),
                new ShopId(entity.getShopId()),
                entity.getCurrency(),
                new Money(
                        entity.getAvailableBalance(),
                        entity.getCurrency()
                ),
                new Money(
                        entity.getPendingBalance(),
                        entity.getCurrency()
                ),
                WalletStatus.valueOf(
                        entity.getStatus()
                )
        );
    }

    public WalletJpaEntity toNewEntity(
            Wallet wallet,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(wallet);
        Objects.requireNonNull(createdAt);

        WalletJpaEntity entity =
                new WalletJpaEntity();

        entity.setId(wallet.id().value());
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);

        copyDomainState(
                wallet,
                entity
        );

        return entity;
    }

    public void updateEntity(
            Wallet wallet,
            WalletJpaEntity entity,
            LocalDateTime updatedAt
    ) {
        Objects.requireNonNull(wallet);
        Objects.requireNonNull(entity);
        Objects.requireNonNull(updatedAt);

        if (!wallet.id().value().equals(entity.getId())) {
            throw new IllegalArgumentException(
                    "wallet id does not match entity id"
            );
        }

        copyDomainState(
                wallet,
                entity
        );

        entity.setUpdatedAt(updatedAt);
    }

    private void copyDomainState(
            Wallet wallet,
            WalletJpaEntity entity
    ) {
        entity.setShopId(
                wallet.shopId().value()
        );

        entity.setCurrency(
                wallet.currency()
        );

        entity.setAvailableBalance(
                wallet.availableBalance().amount()
        );

        entity.setPendingBalance(
                wallet.pendingBalance().amount()
        );

        entity.setStatus(
                wallet.status().name()
        );
    }
}