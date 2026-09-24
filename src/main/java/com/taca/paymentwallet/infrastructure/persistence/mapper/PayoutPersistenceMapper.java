package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.payout.Payout;
import com.taca.paymentwallet.domain.payout.PayoutStatus;
import com.taca.paymentwallet.domain.valueobject.IdempotencyKey;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PayoutId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.infrastructure.persistence.entity.PayoutJpaEntity;

import java.time.LocalDateTime;
import java.util.Objects;

public class PayoutPersistenceMapper {

    private final BankAccountSnapshotPersistenceCodec snapshotCodec;

    public PayoutPersistenceMapper(
            BankAccountSnapshotPersistenceCodec snapshotCodec
    ) {
        this.snapshotCodec =
                Objects.requireNonNull(snapshotCodec);
    }

    public Payout toDomain(
            PayoutJpaEntity entity
    ) {
        Objects.requireNonNull(entity);

        return Payout.rehydrate(
                new PayoutId(entity.getId()),
                new WalletId(entity.getWalletId()),
                new ShopId(entity.getShopId()),
                new Money(
                        entity.getAmount(),
                        entity.getCurrency()
                ),
                snapshotCodec.decode(
                        entity.getBankAccountSnapshot()
                ),
                new IdempotencyKey(
                        entity.getIdempotencyKey()
                ),
                PayoutStatus.valueOf(
                        entity.getStatus()
                ),
                entity.getProviderRef(),
                entity.getFailureCode()
        );
    }

    public PayoutJpaEntity toNewEntity(
            Payout payout,
            LocalDateTime requestedAt
    ) {
        Objects.requireNonNull(payout);
        Objects.requireNonNull(requestedAt);

        PayoutJpaEntity entity =
                new PayoutJpaEntity();

        entity.setId(
                payout.id().value()
        );

        entity.setRequestedAt(requestedAt);
        entity.setUpdatedAt(requestedAt);

        copyDomainState(
                payout,
                entity
        );

        return entity;
    }

    public void updateEntity(
            Payout payout,
            PayoutJpaEntity entity,
            LocalDateTime updatedAt
    ) {
        Objects.requireNonNull(payout);
        Objects.requireNonNull(entity);
        Objects.requireNonNull(updatedAt);

        if (!payout.id().value().equals(entity.getId())) {
            throw new IllegalArgumentException(
                    "payout id does not match entity id"
            );
        }

        copyDomainState(
                payout,
                entity
        );

        entity.setUpdatedAt(updatedAt);
    }

    private void copyDomainState(
            Payout payout,
            PayoutJpaEntity entity
    ) {
        entity.setWalletId(
                payout.walletId().value()
        );

        entity.setShopId(
                payout.shopId().value()
        );

        entity.setAmount(
                payout.amount().amount()
        );

        entity.setCurrency(
                payout.amount().currency()
        );

        entity.setStatus(
                payout.status().name()
        );

        entity.setBankAccountSnapshot(
                snapshotCodec.encode(
                        payout.bankAccountSnapshot()
                )
        );

        entity.setIdempotencyKey(
                payout.idempotencyKey().value()
        );

        entity.setProviderRef(
                payout.providerRef()
        );

        entity.setFailureCode(
                payout.failureCode()
        );
    }
}