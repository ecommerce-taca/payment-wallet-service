package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItem;
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
import com.taca.paymentwallet.infrastructure.persistence.mapper.SettlementPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchItemJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementLineJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class SettlementRepositoryAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-24T17:00:00Z"
            );

    private SettlementBatchJpaRepository batchRepository;
    private SettlementBatchItemJpaRepository itemRepository;
    private SettlementLineJpaRepository lineRepository;
    private ClockPort clockPort;

    private SettlementRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        batchRepository =
                mock(SettlementBatchJpaRepository.class);

        itemRepository =
                mock(SettlementBatchItemJpaRepository.class);

        lineRepository =
                mock(SettlementLineJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(NOW);

        adapter =
                new SettlementRepositoryAdapter(
                        batchRepository,
                        itemRepository,
                        lineRepository,
                        new SettlementPersistenceMapper(),
                        clockPort
                );
    }

    @Test
    void shouldPersistCompletedSettlementHierarchy() {
        SettlementBatch batch =
                completedBatch();

        when(
                lineRepository
                        .existsByPaymentAllocationId(
                                any()
                        )
        ).thenReturn(false);

        SettlementBatch result =
                adapter.save(batch);

        assertSame(
                batch,
                result
        );

        ArgumentCaptor<SettlementBatchJpaEntity>
                batchCaptor =
                ArgumentCaptor.forClass(
                        SettlementBatchJpaEntity.class
                );

        verify(batchRepository)
                .save(
                        batchCaptor.capture()
                );

        SettlementBatchJpaEntity batchEntity =
                batchCaptor.getValue();

        assertEquals(
                batch.id().value(),
                batchEntity.getId()
        );

        assertEquals(
                "COMPLETED",
                batchEntity.getStatus()
        );

        assertEquals(
                1,
                batchEntity.getShopCount()
        );

        assertEquals(
                100_000L,
                batchEntity.getTotalGross()
        );

        assertEquals(
                7_000L,
                batchEntity.getTotalCommission()
        );

        assertEquals(
                1_000L,
                batchEntity.getTotalTax()
        );

        assertEquals(
                92_000L,
                batchEntity.getTotalNet()
        );

        assertEquals(
                92_000L,
                batchEntity.getTotalReleased()
        );

        assertEquals(
                0L,
                batchEntity.getTotalHeld()
        );

        assertEquals(
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        17,
                        0
                ),
                batchEntity.getCreatedAt()
        );

        assertEquals(
                batchEntity.getCreatedAt(),
                batchEntity.getClosedAt()
        );
    }

    @Test
    void shouldPersistSettlementItems() {
        SettlementBatch batch =
                completedBatch();

        when(
                lineRepository
                        .existsByPaymentAllocationId(
                                any()
                        )
        ).thenReturn(false);

        adapter.save(batch);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SettlementBatchItemJpaEntity>>
                captor =
                ArgumentCaptor.forClass(List.class);

        verify(itemRepository)
                .saveAll(
                        captor.capture()
                );

        List<SettlementBatchItemJpaEntity> items =
                captor.getValue();

        assertEquals(
                1,
                items.size()
        );

        SettlementBatchItem original =
                batch.items().getFirst();

        SettlementBatchItemJpaEntity entity =
                items.getFirst();

        assertEquals(
                original.id().value(),
                entity.getId()
        );

        assertEquals(
                batch.id().value(),
                entity.getBatchId()
        );

        assertEquals(
                original.shopId().value(),
                entity.getShopId()
        );

        assertEquals(
                original.walletId().value(),
                entity.getWalletId()
        );

        assertEquals(
                "COMPLETED",
                entity.getStatus()
        );

        assertEquals(
                original.postingId().value(),
                entity.getPostingId()
        );
    }

    @Test
    void shouldPersistSettlementLines() {
        SettlementBatch batch =
                completedBatch();

        when(
                lineRepository
                        .existsByPaymentAllocationId(
                                any()
                        )
        ).thenReturn(false);

        adapter.save(batch);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SettlementLineJpaEntity>>
                captor =
                ArgumentCaptor.forClass(List.class);

        verify(lineRepository)
                .saveAll(
                        captor.capture()
                );

        List<SettlementLineJpaEntity> lines =
                captor.getValue();

        assertEquals(
                1,
                lines.size()
        );

        SettlementLine original =
                batch.items()
                        .getFirst()
                        .lines()
                        .getFirst();

        SettlementLineJpaEntity entity =
                lines.getFirst();

        assertEquals(
                original.id().value(),
                entity.getId()
        );

        assertEquals(
                batch.items()
                        .getFirst()
                        .id()
                        .value(),
                entity.getSettlementBatchItemId()
        );

        assertEquals(
                original.paymentAllocationId().value(),
                entity.getPaymentAllocationId()
        );

        assertEquals(
                original.releasedAmount().amount(),
                entity.getReleasedAmount()
        );
    }

    @Test
    void shouldUseSameTimestampForEntireHierarchy() {
        SettlementBatch batch =
                completedBatch();

        when(
                lineRepository
                        .existsByPaymentAllocationId(
                                any()
                        )
        ).thenReturn(false);

        adapter.save(batch);

        ArgumentCaptor<SettlementBatchJpaEntity>
                batchCaptor =
                ArgumentCaptor.forClass(
                        SettlementBatchJpaEntity.class
                );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SettlementBatchItemJpaEntity>>
                itemCaptor =
                ArgumentCaptor.forClass(List.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SettlementLineJpaEntity>>
                lineCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(batchRepository)
                .save(
                        batchCaptor.capture()
                );

        verify(itemRepository)
                .saveAll(
                        itemCaptor.capture()
                );

        verify(lineRepository)
                .saveAll(
                        lineCaptor.capture()
                );

        LocalDateTime timestamp =
                batchCaptor
                        .getValue()
                        .getCreatedAt();

        assertEquals(
                timestamp,
                itemCaptor
                        .getValue()
                        .getFirst()
                        .getCreatedAt()
        );

        assertEquals(
                timestamp,
                lineCaptor
                        .getValue()
                        .getFirst()
                        .getCreatedAt()
        );

        verify(clockPort, times(1))
                .now();
    }

    @Test
    void shouldRejectAlreadySettledPaymentAllocation() {
        SettlementBatch batch =
                completedBatch();

        PaymentAllocationId allocationId =
                batch.items()
                        .getFirst()
                        .lines()
                        .getFirst()
                        .paymentAllocationId();

        when(
                lineRepository
                        .existsByPaymentAllocationId(
                                allocationId.value()
                        )
        ).thenReturn(true);

        assertThrows(
                PaymentAllocationAlreadySettledException.class,
                () -> adapter.save(batch)
        );

        verify(batchRepository, never())
                .save(any());

        verify(itemRepository, never())
                .saveAll(anyList());

        verify(lineRepository, never())
                .saveAll(anyList());

        verifyNoInteractions(
                clockPort
        );
    }

    private SettlementBatch completedBatch() {
        SettlementLine line =
                new SettlementLine(
                        new SettlementLineId(
                                UUID.randomUUID()
                        ),
                        new PaymentAllocationId(
                                UUID.randomUUID()
                        ),
                        Money.vnd(92_000)
                );

        SettlementBatchItem item =
                SettlementBatchItem.create(
                        new SettlementBatchItemId(
                                UUID.randomUUID()
                        ),
                        new ShopId(
                                UUID.randomUUID()
                        ),
                        new WalletId(
                                UUID.randomUUID()
                        ),
                        Money.vnd(100_000),
                        Money.vnd(7_000),
                        Money.vnd(1_000),
                        Money.vnd(92_000),
                        Money.vnd(92_000),
                        Money.vnd(0),
                        List.of(line)
                );

        SettlementBatch batch =
                SettlementBatch.create(
                        new SettlementBatchId(
                                UUID.randomUUID()
                        ),
                        Instant.parse(
                                "2026-09-01T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-02T00:00:00Z"
                        ),
                        List.of(item)
                );

        batch.markProcessing();

        item.markCompleted(
                new LedgerPostingId(
                        UUID.randomUUID()
                )
        );

        batch.markCompleted();

        batch.clearDomainEvents();

        return batch;
    }

    @Test
    void shouldPersistAllItemsAndLines() {
        SettlementLine firstLine =
                new SettlementLine(
                        new SettlementLineId(UUID.randomUUID()),
                        new PaymentAllocationId(UUID.randomUUID()),
                        Money.vnd(60_000)
                );

        SettlementLine secondLine =
                new SettlementLine(
                        new SettlementLineId(UUID.randomUUID()),
                        new PaymentAllocationId(UUID.randomUUID()),
                        Money.vnd(32_000)
                );

        SettlementBatchItem firstItem =
                SettlementBatchItem.create(
                        new SettlementBatchItemId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        Money.vnd(100_000),
                        Money.vnd(7_000),
                        Money.vnd(1_000),
                        Money.vnd(92_000),
                        Money.vnd(92_000),
                        Money.vnd(0),
                        List.of(
                                firstLine,
                                secondLine
                        )
                );

        SettlementLine thirdLine =
                new SettlementLine(
                        new SettlementLineId(UUID.randomUUID()),
                        new PaymentAllocationId(UUID.randomUUID()),
                        Money.vnd(46_000)
                );

        SettlementBatchItem secondItem =
                SettlementBatchItem.create(
                        new SettlementBatchItemId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        Money.vnd(50_000),
                        Money.vnd(3_500),
                        Money.vnd(500),
                        Money.vnd(46_000),
                        Money.vnd(46_000),
                        Money.vnd(0),
                        List.of(thirdLine)
                );

        SettlementBatch batch =
                SettlementBatch.create(
                        new SettlementBatchId(UUID.randomUUID()),
                        Instant.parse(
                                "2026-09-01T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-02T00:00:00Z"
                        ),
                        List.of(
                                firstItem,
                                secondItem
                        )
                );

        batch.markProcessing();

        firstItem.markCompleted(
                new LedgerPostingId(UUID.randomUUID())
        );

        secondItem.markCompleted(
                new LedgerPostingId(UUID.randomUUID())
        );

        batch.markCompleted();
        batch.clearDomainEvents();

        when(
                lineRepository
                        .existsByPaymentAllocationId(any())
        ).thenReturn(false);

        adapter.save(batch);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SettlementBatchItemJpaEntity>>
                itemCaptor =
                ArgumentCaptor.forClass(List.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SettlementLineJpaEntity>>
                lineCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(itemRepository)
                .saveAll(itemCaptor.capture());

        verify(lineRepository)
                .saveAll(lineCaptor.capture());

        assertEquals(2, itemCaptor.getValue().size());

        assertEquals(3, lineCaptor.getValue().size());
    }
}