package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.RefundId;
import com.taca.paymentwallet.infrastructure.persistence.entity.RefundAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.RefundAllocationPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.RefundAllocationJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundAllocationRepositoryAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-09-24T15:00:00Z");

    private RefundAllocationJpaRepository repository;
    private ClockPort clockPort;

    private RefundAllocationRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository =
                mock(RefundAllocationJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(NOW);

        adapter =
                new RefundAllocationRepositoryAdapter(
                        repository,
                        new RefundAllocationPersistenceMapper(),
                        clockPort,
                        new PersistenceUuidGenerator()
                );
    }

    @Test
    void shouldSaveRefundAllocationsWithRefundId() {
        RefundId refundId =
                new RefundId(UUID.randomUUID());

        RefundAllocation allocation =
                allocation();

        adapter.saveAll(
                refundId,
                List.of(allocation)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RefundAllocationJpaEntity>>
                captor =
                ArgumentCaptor.forClass(List.class);

        verify(repository)
                .saveAll(
                        captor.capture()
                );

        RefundAllocationJpaEntity entity =
                captor.getValue()
                        .getFirst();

        assertEquals(
                refundId.value(),
                entity.getRefundId()
        );

        assertEquals(
                allocation
                        .paymentAllocationId()
                        .value(),
                entity.getPaymentAllocationId()
        );

        assertEquals(
                50_000L,
                entity.getGrossAmount()
        );

        assertEquals(
                3_500L,
                entity.getCommissionReversal()
        );

        assertEquals(
                500L,
                entity.getTaxReversal()
        );

        assertEquals(
                46_000L,
                entity.getSellerReversal()
        );

        assertEquals(
                7,
                entity.getId().version()
        );
    }

    @Test
    void shouldUseSameTimestampForEntireRefundAllocationBatch() {
        RefundId refundId =
                new RefundId(UUID.randomUUID());

        adapter.saveAll(
                refundId,
                List.of(
                        allocation(),
                        allocation()
                )
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RefundAllocationJpaEntity>>
                captor =
                ArgumentCaptor.forClass(List.class);

        verify(repository)
                .saveAll(
                        captor.capture()
                );

        List<RefundAllocationJpaEntity> entities =
                captor.getValue();

        assertEquals(
                entities.get(0).getCreatedAt(),
                entities.get(1).getCreatedAt()
        );

        verify(clockPort, times(1))
                .now();
    }

    @Test
    void shouldDoNothingForEmptyAllocationList() {
        adapter.saveAll(
                new RefundId(UUID.randomUUID()),
                List.of()
        );

        verifyNoInteractions(repository);
        verifyNoInteractions(clockPort);
    }

    private RefundAllocation allocation() {
        return new RefundAllocation(
                new PaymentAllocationId(
                        UUID.randomUUID()
                ),
                Money.vnd(50_000),
                Money.vnd(3_500),
                Money.vnd(500),
                Money.vnd(46_000)
        );
    }
}