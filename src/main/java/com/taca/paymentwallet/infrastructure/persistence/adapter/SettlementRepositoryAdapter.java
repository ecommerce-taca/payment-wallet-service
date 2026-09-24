package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.SettlementRepositoryPort;
import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItem;
import com.taca.paymentwallet.domain.settlement.SettlementBatchStatus;
import com.taca.paymentwallet.domain.settlement.SettlementLine;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchItemJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementLineJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.SettlementPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchItemJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementLineJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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