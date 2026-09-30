package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.payment.PaymentAllocation;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PaymentAllocationPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;
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

class PaymentAllocationRepositoryAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-24T15:00:00Z"
            );

    private PaymentAllocationJpaRepository repository;
    private ClockPort clockPort;

    private PaymentAllocationRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository =
                mock(PaymentAllocationJpaRepository.class);

        clockPort =
                mock(ClockPort.class);

        when(clockPort.now())
                .thenReturn(NOW);

        adapter =
                new PaymentAllocationRepositoryAdapter(
                        repository,
                        new PaymentAllocationPersistenceMapper(),
                        clockPort
                );
    }

    @Test
    void shouldSaveAllPaymentAllocations() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        PaymentAllocation first =
                allocation(
                        paymentId,
                        100_000,
                        7_000,
                        1_000,
                        92_000
                );

        PaymentAllocation second =
                allocation(
                        paymentId,
                        50_000,
                        3_500,
                        500,
                        46_000
                );

        adapter.saveAll(
                List.of(
                        first,
                        second
                )
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PaymentAllocationJpaEntity>>
                captor =
                ArgumentCaptor.forClass(List.class);

        verify(repository)
                .saveAll(
                        captor.capture()
                );

        List<PaymentAllocationJpaEntity> entities =
                captor.getValue();

        assertEquals(
                2,
                entities.size()
        );

        PaymentAllocationJpaEntity firstEntity =
                entities.get(0);

        assertEquals(
                first.id().value(),
                firstEntity.getId()
        );

        assertEquals(
                first.paymentId().value(),
                firstEntity.getPaymentId()
        );

        assertEquals(
                first.walletId().value(),
                firstEntity.getWalletId()
        );

        assertEquals(
                first.feeConfigId().value(),
                firstEntity.getFeeConfigId()
        );

        assertEquals(
                first.taxConfigId().value(),
                firstEntity.getTaxConfigId()
        );

        assertEquals(
                100_000L,
                firstEntity.getGrossAmount()
        );

        assertEquals(
                92_000L,
                firstEntity.getSellerNetAmount()
        );
    }

    @Test
    void shouldUseSameCreatedAtForAllAllocations() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        adapter.saveAll(
                List.of(
                        allocation(
                                paymentId,
                                100_000,
                                7_000,
                                1_000,
                                92_000
                        ),
                        allocation(
                                paymentId,
                                50_000,
                                3_500,
                                500,
                                46_000
                        )
                )
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PaymentAllocationJpaEntity>>
                captor =
                ArgumentCaptor.forClass(List.class);

        verify(repository)
                .saveAll(
                        captor.capture()
                );

        List<PaymentAllocationJpaEntity> entities =
                captor.getValue();

        LocalDateTime expected =
                LocalDateTime.of(
                        2026,
                        9,
                        24,
                        15,
                        0
                );

        assertEquals(
                expected,
                entities.get(0).getCreatedAt()
        );

        assertEquals(
                expected,
                entities.get(1).getCreatedAt()
        );

        verify(clockPort, times(1))
                .now();
    }

    @Test
    void shouldFindAllocationsByPaymentId() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        PaymentAllocation first =
                allocation(
                        paymentId,
                        100_000,
                        7_000,
                        1_000,
                        92_000
                );

        PaymentAllocation second =
                allocation(
                        paymentId,
                        50_000,
                        3_500,
                        500,
                        46_000
                );

        PaymentAllocationPersistenceMapper mapper =
                new PaymentAllocationPersistenceMapper();

        PaymentAllocationJpaEntity firstEntity =
                mapper.toEntity(
                        first,
                        LocalDateTime.of(
                                2026,
                                9,
                                24,
                                15,
                                0
                        )
                );

        PaymentAllocationJpaEntity secondEntity =
                mapper.toEntity(
                        second,
                        LocalDateTime.of(
                                2026,
                                9,
                                24,
                                15,
                                1
                        )
                );

        when(
                repository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId.value()
                        )
        ).thenReturn(
                List.of(
                        firstEntity,
                        secondEntity
                )
        );

        List<PaymentAllocation> result =
                adapter.findByPaymentId(
                        paymentId
                );

        assertEquals(
                2,
                result.size()
        );

        assertEquals(
                first,
                result.get(0)
        );

        assertEquals(
                second,
                result.get(1)
        );

        verify(repository)
                .findByPaymentIdOrderByCreatedAtAscIdAsc(
                        paymentId.value()
                );
    }

    @Test
    void shouldReturnEmptyListWhenPaymentHasNoAllocations() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        when(
                repository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId.value()
                        )
        ).thenReturn(
                List.of()
        );

        List<PaymentAllocation> result =
                adapter.findByPaymentId(
                        paymentId
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldNotCallRepositoryWhenSavingEmptyList() {
        adapter.saveAll(
                List.of()
        );

        verify(repository, never())
                .saveAll(anyList());

        verifyNoInteractions(
                clockPort
        );
    }

    @Test
    void shouldRejectNullAllocationList() {
        assertThrows(
                NullPointerException.class,
                () -> adapter.saveAll(null)
        );

        verifyNoInteractions(repository);
        verifyNoInteractions(clockPort);
    }

    @Test
    void shouldRejectNullPaymentId() {
        assertThrows(
                NullPointerException.class,
                () -> adapter.findByPaymentId(null)
        );

        verifyNoInteractions(repository);
    }

    private PaymentAllocation allocation(
            PaymentId paymentId,
            long gross,
            long commission,
            long tax,
            long sellerNet
    ) {
        return new PaymentAllocation(
                new PaymentAllocationId(
                        UUID.randomUUID()
                ),
                paymentId,
                new OrderId(
                        UUID.randomUUID()
                ),
                new ShopId(
                        UUID.randomUUID()
                ),
                new WalletId(
                        UUID.randomUUID()
                ),
                Money.vnd(gross),
                Money.vnd(commission),
                Money.vnd(tax),
                Money.vnd(sellerNet),
                new FeeConfigId(
                        UUID.randomUUID()
                ),
                new TaxConfigId(
                        UUID.randomUUID()
                )
        );
    }
}