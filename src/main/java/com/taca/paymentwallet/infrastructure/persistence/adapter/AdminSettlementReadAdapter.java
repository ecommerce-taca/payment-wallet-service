package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchItemJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementLineJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchItemJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementLineJpaRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class AdminSettlementReadAdapter implements AdminSettlementReadPort {

    private final SettlementBatchJpaRepository batchRepository;
    private final SettlementBatchItemJpaRepository itemRepository;
    private final SettlementLineJpaRepository lineRepository;

    public AdminSettlementReadAdapter(
            SettlementBatchJpaRepository batchRepository,
            SettlementBatchItemJpaRepository itemRepository,
            SettlementLineJpaRepository lineRepository
    ) {
        this.batchRepository = Objects.requireNonNull(batchRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.lineRepository = Objects.requireNonNull(lineRepository);
    }

    @Override
    public List<AdminSettlementBatchView> findAll() {
        return batchRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toBatchView)
                .toList();
    }

    @Override
    public Optional<AdminSettlementBatchDetailView> findDetailById(UUID batchId) {
        Objects.requireNonNull(batchId, "batchId must not be null");

        return batchRepository.findById(batchId)
                .map(batch -> {
                    List<AdminSettlementBatchItemDetailView> items =
                            itemRepository
                                    .findByBatchIdOrderByCreatedAtAsc(batchId)
                                    .stream()
                                    .map(this::toItemDetailView)
                                    .toList();

                    return new AdminSettlementBatchDetailView(
                            toBatchView(batch),
                            items
                    );
                });
    }

    private AdminSettlementBatchItemDetailView toItemDetailView(
            SettlementBatchItemJpaEntity entity
    ) {
        List<AdminSettlementLineView> lines =
                lineRepository
                        .findBySettlementBatchItemIdOrderByCreatedAtAsc(entity.getId())
                        .stream()
                        .map(this::toLineView)
                        .toList();

        return new AdminSettlementBatchItemDetailView(
                toItemView(entity),
                lines
        );
    }

    private AdminSettlementBatchView toBatchView(
            SettlementBatchJpaEntity entity
    ) {
        return new AdminSettlementBatchView(
                entity.getId(),
                PersistenceTimeMapper.toInstant(entity.getPeriodStart()),
                PersistenceTimeMapper.toInstant(entity.getPeriodEnd()),
                entity.getStatus(),
                entity.getShopCount(),
                entity.getTotalGross(),
                entity.getTotalCommission(),
                entity.getTotalTax(),
                entity.getTotalNet(),
                entity.getTotalReleased(),
                entity.getTotalHeld(),
                PersistenceTimeMapper.toInstant(entity.getCreatedAt()),
                PersistenceTimeMapper.toInstant(entity.getClosedAt()),
                entity.getLastError()
        );
    }

    private AdminSettlementBatchItemView toItemView(
            SettlementBatchItemJpaEntity entity
    ) {
        return new AdminSettlementBatchItemView(
                entity.getId(),
                entity.getBatchId(),
                entity.getShopId(),
                entity.getWalletId(),
                entity.getGross(),
                entity.getCommission(),
                entity.getTax(),
                entity.getNet(),
                entity.getReleasedAmount(),
                entity.getHeldAmount(),
                entity.getStatus(),
                entity.getPostingId(),
                PersistenceTimeMapper.toInstant(entity.getCreatedAt())
        );
    }

    private AdminSettlementLineView toLineView(
            SettlementLineJpaEntity entity
    ) {
        return new AdminSettlementLineView(
                entity.getId(),
                entity.getSettlementBatchItemId(),
                entity.getPaymentAllocationId(),
                entity.getReleasedAmount(),
                PersistenceTimeMapper.toInstant(entity.getCreatedAt())
        );
    }
}