package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.SettlementRepositoryPort;
import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItem;
import com.taca.paymentwallet.domain.settlement.SettlementBatchStatus;
import com.taca.paymentwallet.domain.settlement.SettlementLine;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchItemJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementLineJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.SettlementPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchItemJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementLineJpaRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class SettlementRepositoryAdapter
        implements SettlementRepositoryPort {

    private final SettlementBatchJpaRepository batchRepository;
    private final SettlementBatchItemJpaRepository itemRepository;
    private final SettlementLineJpaRepository lineRepository;
    private final SettlementPersistenceMapper mapper;
    private final ClockPort clockPort;

    public SettlementRepositoryAdapter(
            SettlementBatchJpaRepository batchRepository,
            SettlementBatchItemJpaRepository itemRepository,
            SettlementLineJpaRepository lineRepository,
            SettlementPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        this.batchRepository =
                Objects.requireNonNull(batchRepository);

        this.itemRepository =
                Objects.requireNonNull(itemRepository);

        this.lineRepository =
                Objects.requireNonNull(lineRepository);

        this.mapper =
                Objects.requireNonNull(mapper);

        this.clockPort =
                Objects.requireNonNull(clockPort);
    }

    @Override
    public SettlementBatch save(
            SettlementBatch settlementBatch
    ) {
        Objects.requireNonNull(
                settlementBatch,
                "settlementBatch must not be null"
        );

        ensureAllocationsAreNotAlreadySettled(
                settlementBatch
        );

        LocalDateTime now =
                PersistenceTimeMapper.toLocalDateTime(
                        clockPort.now()
                );

        SettlementBatchJpaEntity batchEntity =
                mapper.toBatchEntity(
                        settlementBatch,
                        now,
                        resolveClosedAt(
                                settlementBatch,
                                now
                        ),
                        null
                );

        List<SettlementBatchItemJpaEntity> itemEntities =
                settlementBatch.items()
                        .stream()
                        .map(item ->
                                mapper.toItemEntity(
                                        settlementBatch.id(),
                                        item,
                                        now
                                )
                        )
                        .toList();

        List<SettlementLineJpaEntity> lineEntities =
                settlementBatch.items()
                        .stream()
                        .flatMap(item ->
                                item.lines()
                                        .stream()
                                        .map(line ->
                                                mapper.toLineEntity(
                                                        item.id(),
                                                        line,
                                                        now
                                                )
                                        )
                        )
                        .toList();

        batchRepository.save(batchEntity);
        itemRepository.saveAll(itemEntities);
        lineRepository.saveAll(lineEntities);

        return settlementBatch;
    }

    @Override
    public Optional<SettlementBatch> findByIdForUpdate(
            SettlementBatchId settlementBatchId
    ) {
        Objects.requireNonNull(
                settlementBatchId,
                "settlementBatchId must not be null"
        );

        return batchRepository
                .findByIdForUpdate(
                        settlementBatchId.value()
                )
                .map(this::loadHierarchy);
    }

    @Override
    public SettlementBatch update(
            SettlementBatch settlementBatch
    ) {
        Objects.requireNonNull(
                settlementBatch,
                "settlementBatch must not be null"
        );

        SettlementBatchJpaEntity batchEntity =
                batchRepository
                        .findById(
                                settlementBatch.id().value()
                        )
                        .orElseThrow();

        batchEntity.setStatus(
                settlementBatch.status().name()
        );

        batchEntity.setClosedAt(
                resolveClosedAtForUpdate(
                        settlementBatch
                )
        );

        if (settlementBatch.status()
                == SettlementBatchStatus.PROCESSING) {
            batchEntity.setLastError(null);
        }

        List<SettlementBatchItemJpaEntity> itemEntities =
                itemRepository
                        .findByBatchId(
                                settlementBatch.id().value()
                        );

        Map<UUID, SettlementBatchItem> itemsById =
                settlementBatch.items()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        item -> item.id().value(),
                                        item -> item
                                )
                        );

        for (SettlementBatchItemJpaEntity entity : itemEntities) {

            SettlementBatchItem item = itemsById.get(entity.getId());

            if (item == null) {
                throw new IllegalStateException(
                        "Settlement batch item not found in aggregate: "
                                + entity.getId()
                );
            }

            entity.setStatus(item.status().name());

            entity.setPostingId(
                    item.postingId() == null
                            ? null
                            : item.postingId().value()
            );
        }

        batchRepository.save(batchEntity);

        itemRepository.saveAll(itemEntities);

        return settlementBatch;
    }

    private LocalDateTime resolveClosedAtForUpdate(
            SettlementBatch batch
    ) {
        if (batch.status()
                == SettlementBatchStatus.COMPLETED
                || batch.status()
                == SettlementBatchStatus.FAILED) {

            return PersistenceTimeMapper
                    .toLocalDateTime(
                            clockPort.now()
                    );
        }

        return null;
    }

    private SettlementBatch loadHierarchy(
            SettlementBatchJpaEntity batchEntity
    ) {
        List<SettlementBatchItemJpaEntity> itemEntities =
                itemRepository
                        .findByBatchIdOrderByCreatedAtAsc(
                                batchEntity.getId()
                        );

        Map<UUID, List<SettlementLineJpaEntity>>
                linesByItemId =
                itemEntities.stream()
                        .collect(
                                Collectors.toMap(
                                        SettlementBatchItemJpaEntity::getId,
                                        item -> lineRepository
                                                .findBySettlementBatchItemIdOrderByCreatedAtAsc(
                                                        item.getId()
                                                )
                                )
                        );

        return mapper.toDomain(
                batchEntity,
                itemEntities,
                linesByItemId
        );
    }

    private void ensureAllocationsAreNotAlreadySettled(
            SettlementBatch batch
    ) {
        for (SettlementBatchItem item : batch.items()) {
            for (SettlementLine line : item.lines()) {

                if (lineRepository
                        .existsByPaymentAllocationId(
                                line.paymentAllocationId()
                                        .value()
                        )) {

                    throw new PaymentAllocationAlreadySettledException(
                            line.paymentAllocationId()
                                    .value()
                    );
                }
            }
        }
    }

    private LocalDateTime resolveClosedAt(
            SettlementBatch batch,
            LocalDateTime now
    ) {
        if (batch.status() == SettlementBatchStatus.COMPLETED
                || batch.status() == SettlementBatchStatus.FAILED) {

            return now;
        }

        return null;
    }
}