package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItem;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItemStatus;
import com.taca.paymentwallet.domain.settlement.SettlementBatchStatus;
import com.taca.paymentwallet.domain.settlement.SettlementLine;
import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchItemId;
import com.taca.paymentwallet.domain.valueobject.SettlementLineId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchItemJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementLineJpaEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SettlementPersistenceMapper {

    public SettlementBatch toDomain(
            SettlementBatchJpaEntity batchEntity,
            List<SettlementBatchItemJpaEntity> itemEntities,
            Map<UUID, List<SettlementLineJpaEntity>> linesByItemId
    ) {
        Objects.requireNonNull(batchEntity);
        Objects.requireNonNull(itemEntities);
        Objects.requireNonNull(linesByItemId);

        List<SettlementBatchItem> items =
                itemEntities.stream()
                        .map(entity -> toDomainItem(
                                entity,
                                linesByItemId.getOrDefault(
                                        entity.getId(),
                                        List.of()
                                )
                        ))
                        .toList();

        return SettlementBatch.rehydrate(
                new SettlementBatchId(
                        batchEntity.getId()
                ),
                PersistenceTimeMapper.toInstant(
                        batchEntity.getPeriodStart()
                ),
                PersistenceTimeMapper.toInstant(
                        batchEntity.getPeriodEnd()
                ),
                items,
                SettlementBatchStatus.valueOf(
                        batchEntity.getStatus()
                )
        );
    }

    public SettlementBatchItem toDomainItem(
            SettlementBatchItemJpaEntity entity,
            List<SettlementLineJpaEntity> lineEntities
    ) {
        Objects.requireNonNull(entity);
        Objects.requireNonNull(lineEntities);

        List<SettlementLine> lines =
                lineEntities.stream()
                        .map(this::toDomainLine)
                        .toList();

        LedgerPostingId postingId =
                entity.getPostingId() == null
                        ? null
                        : new LedgerPostingId(
                        entity.getPostingId()
                );

        return SettlementBatchItem.rehydrate(
                new SettlementBatchItemId(
                        entity.getId()
                ),
                new ShopId(
                        entity.getShopId()
                ),
                new WalletId(
                        entity.getWalletId()
                ),
                Money.vnd(entity.getGross()),
                Money.vnd(entity.getCommission()),
                Money.vnd(entity.getTax()),
                Money.vnd(entity.getNet()),
                Money.vnd(entity.getReleasedAmount()),
                Money.vnd(entity.getHeldAmount()),
                lines,
                SettlementBatchItemStatus.valueOf(
                        entity.getStatus()
                ),
                postingId
        );
    }

    public SettlementLine toDomainLine(
            SettlementLineJpaEntity entity
    ) {
        Objects.requireNonNull(entity);

        return new SettlementLine(
                new SettlementLineId(
                        entity.getId()
                ),
                new PaymentAllocationId(
                        entity.getPaymentAllocationId()
                ),
                Money.vnd(
                        entity.getReleasedAmount()
                )
        );
    }

    public SettlementBatchJpaEntity toBatchEntity(
            SettlementBatch batch,
            LocalDateTime createdAt,
            LocalDateTime closedAt,
            String lastError
    ) {
        Objects.requireNonNull(batch);
        Objects.requireNonNull(createdAt);

        SettlementBatchJpaEntity entity =
                new SettlementBatchJpaEntity();

        entity.setId(
                batch.id().value()
        );

        entity.setPeriodStart(
                PersistenceTimeMapper.toLocalDateTime(
                        batch.periodStart()
                )
        );

        entity.setPeriodEnd(
                PersistenceTimeMapper.toLocalDateTime(
                        batch.periodEnd()
                )
        );

        entity.setStatus(
                batch.status().name()
        );

        entity.setShopCount(
                Math.toIntExact(
                        batch.items()
                                .stream()
                                .map(SettlementBatchItem::shopId)
                                .distinct()
                                .count()
                )
        );

        entity.setTotalGross(
                sumGross(batch)
        );

        entity.setTotalCommission(
                sumCommission(batch)
        );

        entity.setTotalTax(
                sumTax(batch)
        );

        entity.setTotalNet(
                sumNet(batch)
        );

        entity.setTotalReleased(
                sumReleased(batch)
        );

        entity.setTotalHeld(
                sumHeld(batch)
        );

        entity.setCreatedAt(createdAt);
        entity.setClosedAt(closedAt);
        entity.setLastError(lastError);

        return entity;
    }

    public SettlementBatchItemJpaEntity toItemEntity(
            SettlementBatchId batchId,
            SettlementBatchItem item,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(batchId);
        Objects.requireNonNull(item);
        Objects.requireNonNull(createdAt);

        SettlementBatchItemJpaEntity entity =
                new SettlementBatchItemJpaEntity();

        entity.setId(
                item.id().value()
        );

        entity.setBatchId(
                batchId.value()
        );

        entity.setShopId(
                item.shopId().value()
        );

        entity.setWalletId(
                item.walletId().value()
        );

        entity.setGross(
                item.gross().amount()
        );

        entity.setCommission(
                item.commission().amount()
        );

        entity.setTax(
                item.tax().amount()
        );

        entity.setNet(
                item.net().amount()
        );

        entity.setReleasedAmount(
                item.releasedAmount().amount()
        );

        entity.setHeldAmount(
                item.heldAmount().amount()
        );

        entity.setStatus(
                item.status().name()
        );

        entity.setPostingId(
                item.postingId() == null
                        ? null
                        : item.postingId().value()
        );

        entity.setCreatedAt(createdAt);

        return entity;
    }

    public SettlementLineJpaEntity toLineEntity(
            SettlementBatchItemId itemId,
            SettlementLine line,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(itemId);
        Objects.requireNonNull(line);
        Objects.requireNonNull(createdAt);

        SettlementLineJpaEntity entity =
                new SettlementLineJpaEntity();

        entity.setId(
                line.id().value()
        );

        entity.setSettlementBatchItemId(
                itemId.value()
        );

        entity.setPaymentAllocationId(
                line.paymentAllocationId().value()
        );

        entity.setReleasedAmount(
                line.releasedAmount().amount()
        );

        entity.setCreatedAt(createdAt);

        return entity;
    }

    private long sumGross(
            SettlementBatch batch
    ) {
        return batch.items().stream()
                .map(SettlementBatchItem::gross)
                .mapToLong(Money::amount)
                .sum();
    }

    private long sumCommission(
            SettlementBatch batch
    ) {
        return batch.items().stream()
                .map(SettlementBatchItem::commission)
                .mapToLong(Money::amount)
                .sum();
    }

    private long sumTax(
            SettlementBatch batch
    ) {
        return batch.items().stream()
                .map(SettlementBatchItem::tax)
                .mapToLong(Money::amount)
                .sum();
    }

    private long sumNet(
            SettlementBatch batch
    ) {
        return batch.items().stream()
                .map(SettlementBatchItem::net)
                .mapToLong(Money::amount)
                .sum();
    }

    private long sumReleased(
            SettlementBatch batch
    ) {
        return batch.items().stream()
                .map(SettlementBatchItem::releasedAmount)
                .mapToLong(Money::amount)
                .sum();
    }

    private long sumHeld(
            SettlementBatch batch
    ) {
        return batch.items().stream()
                .map(SettlementBatchItem::heldAmount)
                .mapToLong(Money::amount)
                .sum();
    }
}