package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.settlement.UnsettledSettlementCandidate;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.infrastructure.persistence.projection.SettlementCandidateProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettlementCandidatePersistenceAdapterTest {

    @Test
    void shouldMapUnsettledAllocationToCandidate() {
        PaymentAllocationJpaRepository repository =
                mock(
                        PaymentAllocationJpaRepository.class
                );

        SettlementCandidateProjection projection =
                mock(
                        SettlementCandidateProjection.class
                );

        UUID allocationId =
                UUID.randomUUID();

        UUID shopId =
                UUID.randomUUID();

        UUID walletId =
                UUID.randomUUID();

        when(
                projection.getPaymentAllocationId()
        ).thenReturn(
                allocationId
        );

        when(
                projection.getShopId()
        ).thenReturn(
                shopId
        );

        when(
                projection.getWalletId()
        ).thenReturn(
                walletId
        );

        when(
                projection.getGrossAmount()
        ).thenReturn(
                100_000L
        );

        when(
                projection.getCommissionAmount()
        ).thenReturn(
                7_000L
        );

        when(
                projection.getTaxAmount()
        ).thenReturn(
                1_000L
        );

        when(
                projection.getSellerNetAmount()
        ).thenReturn(
                92_000L
        );

        when(
                projection.getCurrency()
        ).thenReturn(
                "VND"
        );

        Instant periodStart =
                Instant.parse(
                        "2026-09-01T00:00:00Z"
                );

        Instant periodEnd =
                Instant.parse(
                        "2026-09-02T00:00:00Z"
                );

        when(
                repository.findUnsettledCandidates(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                )
        ).thenReturn(
                List.of(
                        projection
                )
        );

        when(
                projection.getCreatedAt()
        ).thenReturn(
                java.time.LocalDateTime.of(
                        2026,
                        9,
                        1,
                        10,
                        5
                )
        );

        SettlementCandidatePersistenceAdapter adapter =
                new SettlementCandidatePersistenceAdapter(
                        repository
                );

        List<UnsettledSettlementCandidate> result =
                adapter.findUnsettledCandidates(
                        periodStart,
                        periodEnd
                );

        assertThat(
                result
        ).hasSize(
                1
        );

        UnsettledSettlementCandidate candidate = result.getFirst();

        assertThat(
                candidate.paymentAllocationId()
                        .value()
        ).isEqualTo(
                allocationId
        );

        assertThat(
                candidate.shopId()
                        .value()
        ).isEqualTo(
                shopId
        );

        assertThat(
                candidate.walletId()
                        .value()
        ).isEqualTo(
                walletId
        );

        assertThat(
                candidate.grossAmount()
        ).isEqualTo(
                Money.vnd(
                        100_000
                )
        );

        assertThat(
                candidate.commissionAmount()
        ).isEqualTo(
                Money.vnd(
                        7_000
                )
        );

        assertThat(
                candidate.taxAmount()
        ).isEqualTo(
                Money.vnd(
                        1_000
                )
        );

        assertThat(
                candidate.sellerNetAmount()
        ).isEqualTo(
                Money.vnd(
                        92_000
                )
        );

        assertThat(
                candidate.allocationCreatedAt()
        ).isEqualTo(
                Instant.parse(
                        "2026-09-01T10:05:00Z"
                )
        );
    }

    @Test
    void shouldRejectInvalidPeriod() {
        PaymentAllocationJpaRepository repository =
                mock(
                        PaymentAllocationJpaRepository.class
                );

        SettlementCandidatePersistenceAdapter adapter =
                new SettlementCandidatePersistenceAdapter(
                        repository
                );

        Instant now =
                Instant.parse(
                        "2026-09-01T00:00:00Z"
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adapter.findUnsettledCandidates(
                                now,
                                now
                        )
        );
    }
}