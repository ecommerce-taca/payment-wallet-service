package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminSettlementBatchDetailView;
import com.taca.paymentwallet.application.port.out.AdminSettlementBatchView;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchItemJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementBatchJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.SettlementLineJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchItemJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementBatchJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.SettlementLineJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSettlementReadAdapterTest {

    private SettlementBatchJpaRepository batchRepository;
    private SettlementBatchItemJpaRepository itemRepository;
    private SettlementLineJpaRepository lineRepository;
    private AdminSettlementReadAdapter adapter;

    @BeforeEach
    void setUp() {
        batchRepository = mock(SettlementBatchJpaRepository.class);
        itemRepository = mock(SettlementBatchItemJpaRepository.class);
        lineRepository = mock(SettlementLineJpaRepository.class);

        adapter = new AdminSettlementReadAdapter(
                batchRepository,
                itemRepository,
                lineRepository
        );
    }

    @Test
    void shouldReturnSettlementBatchesInRepositoryOrder() {
        SettlementBatchJpaEntity batch = batch();

        when(batchRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(batch));

        List<AdminSettlementBatchView> result = adapter.findAll();

        assertEquals(1, result.size());
        assertEquals(batch.getId(), result.get(0).id());
        assertEquals("COMPLETED", result.get(0).status());
        assertEquals(1, result.get(0).shopCount());
        assertEquals(1_000_000L, result.get(0).totalGross());
        assertEquals(850_000L, result.get(0).totalNet());

        verify(batchRepository).findAllByOrderByCreatedAtDesc();
        verifyNoInteractions(itemRepository);
        verifyNoInteractions(lineRepository);
    }

    @Test
    void shouldReturnSettlementBatchDetail() {
        SettlementBatchJpaEntity batch = batch();
        SettlementBatchItemJpaEntity item = item(batch.getId());
        SettlementLineJpaEntity line = line(item.getId());

        when(batchRepository.findById(batch.getId()))
                .thenReturn(Optional.of(batch));

        when(itemRepository.findByBatchIdOrderByCreatedAtAsc(batch.getId()))
                .thenReturn(List.of(item));

        when(lineRepository.findBySettlementBatchItemIdOrderByCreatedAtAsc(item.getId()))
                .thenReturn(List.of(line));

        Optional<AdminSettlementBatchDetailView> result =
                adapter.findDetailById(batch.getId());

        assertTrue(result.isPresent());

        AdminSettlementBatchDetailView detail = result.orElseThrow();

        assertEquals(batch.getId(), detail.batch().id());
        assertEquals(1, detail.items().size());
        assertEquals(item.getId(), detail.items().get(0).item().id());
        assertEquals(1, detail.items().get(0).lines().size());
        assertEquals(
                line.getPaymentAllocationId(),
                detail.items().get(0).lines().get(0).paymentAllocationId()
        );
    }

    @Test
    void shouldReturnEmptyWhenSettlementBatchDoesNotExist() {
        UUID batchId = UUID.randomUUID();

        when(batchRepository.findById(batchId))
                .thenReturn(Optional.empty());

        assertTrue(adapter.findDetailById(batchId).isEmpty());

        verify(batchRepository).findById(batchId);
        verifyNoInteractions(itemRepository);
        verifyNoInteractions(lineRepository);
    }

    private SettlementBatchJpaEntity batch() {
        SettlementBatchJpaEntity entity = new SettlementBatchJpaEntity();

        entity.setId(UUID.randomUUID());
        entity.setPeriodStart(LocalDateTime.of(2026, 10, 1, 0, 0));
        entity.setPeriodEnd(LocalDateTime.of(2026, 10, 8, 0, 0));
        entity.setStatus("COMPLETED");
        entity.setShopCount(1);
        entity.setTotalGross(1_000_000L);
        entity.setTotalCommission(100_000L);
        entity.setTotalTax(50_000L);
        entity.setTotalNet(850_000L);
        entity.setTotalReleased(850_000L);
        entity.setTotalHeld(0L);
        entity.setCreatedAt(LocalDateTime.of(2026, 10, 8, 1, 0));
        entity.setClosedAt(LocalDateTime.of(2026, 10, 8, 1, 5));
        entity.setLastError(null);

        return entity;
    }

    private SettlementBatchItemJpaEntity item(UUID batchId) {
        SettlementBatchItemJpaEntity entity =
                new SettlementBatchItemJpaEntity();

        entity.setId(UUID.randomUUID());
        entity.setBatchId(batchId);
        entity.setShopId(UUID.randomUUID());
        entity.setWalletId(UUID.randomUUID());
        entity.setGross(1_000_000L);
        entity.setCommission(100_000L);
        entity.setTax(50_000L);
        entity.setNet(850_000L);
        entity.setReleasedAmount(850_000L);
        entity.setHeldAmount(0L);
        entity.setStatus("COMPLETED");
        entity.setPostingId(UUID.randomUUID());
        entity.setCreatedAt(LocalDateTime.of(2026, 10, 8, 1, 0));

        return entity;
    }

    private SettlementLineJpaEntity line(UUID itemId) {
        SettlementLineJpaEntity entity =
                new SettlementLineJpaEntity();

        entity.setId(UUID.randomUUID());
        entity.setSettlementBatchItemId(itemId);
        entity.setPaymentAllocationId(UUID.randomUUID());
        entity.setReleasedAmount(850_000L);
        entity.setCreatedAt(LocalDateTime.of(2026, 10, 8, 1, 0));

        return entity;
    }
}