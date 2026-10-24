package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryView;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.projection.AdminFinanceSummaryProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.AdminFinanceSummaryJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminFinanceSummaryReadAdapterTest {

    @Test
    void shouldMapFinanceSummary() {
        AdminFinanceSummaryJpaRepository repository =
                mock(AdminFinanceSummaryJpaRepository.class);

        ClockPort clockPort = mock(ClockPort.class);

        AdminFinanceSummaryProjection projection =
                projection(
                        1_000_000L,
                        100_000L,
                        900_000L,
                        90_000L,
                        45_000L,
                        765_000L,
                        500_000L,
                        265_000L,
                        200_000L
                );

        Instant now =
                Instant.parse("2026-10-23T08:00:00Z");

        when(repository.getFinanceSummary())
                .thenReturn(projection);

        when(clockPort.now())
                .thenReturn(now);

        AdminFinanceSummaryReadAdapter adapter =
                new AdminFinanceSummaryReadAdapter(
                        repository,
                        clockPort
                );

        AdminFinanceSummaryView result =
                adapter.getSummary();

        assertThat(result.capturedAmount())
                .isEqualTo(1_000_000L);

        assertThat(result.refundedAmount())
                .isEqualTo(100_000L);

        assertThat(result.grossAmount())
                .isEqualTo(900_000L);

        assertThat(result.commissionAmount())
                .isEqualTo(90_000L);

        assertThat(result.taxAmount())
                .isEqualTo(45_000L);

        assertThat(result.sellerNetAmount())
                .isEqualTo(765_000L);

        assertThat(result.availableBalance())
                .isEqualTo(500_000L);

        assertThat(result.pendingBalance())
                .isEqualTo(265_000L);

        assertThat(result.completedPayoutAmount())
                .isEqualTo(200_000L);

        assertThat(result.asOf())
                .isEqualTo(now);

        verify(repository).getFinanceSummary();
        verify(clockPort).now();
    }

    @Test
    void shouldNormalizeNullableProjectionValuesToZero() {
        AdminFinanceSummaryJpaRepository repository =
                mock(AdminFinanceSummaryJpaRepository.class);

        ClockPort clockPort = mock(ClockPort.class);

        when(repository.getFinanceSummary())
                .thenReturn(
                        projection(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                );

        when(clockPort.now())
                .thenReturn(
                        Instant.parse("2026-10-23T08:00:00Z")
                );

        AdminFinanceSummaryReadAdapter adapter =
                new AdminFinanceSummaryReadAdapter(
                        repository,
                        clockPort
                );

        AdminFinanceSummaryView result =
                adapter.getSummary();

        assertThat(result.capturedAmount()).isZero();
        assertThat(result.refundedAmount()).isZero();
        assertThat(result.grossAmount()).isZero();
        assertThat(result.commissionAmount()).isZero();
        assertThat(result.taxAmount()).isZero();
        assertThat(result.sellerNetAmount()).isZero();
        assertThat(result.availableBalance()).isZero();
        assertThat(result.pendingBalance()).isZero();
        assertThat(result.completedPayoutAmount()).isZero();
    }

    private AdminFinanceSummaryProjection projection(
            Long capturedAmount,
            Long refundedAmount,
            Long grossAmount,
            Long commissionAmount,
            Long taxAmount,
            Long sellerNetAmount,
            Long availableBalance,
            Long pendingBalance,
            Long completedPayoutAmount
    ) {
        return new AdminFinanceSummaryProjection() {

            @Override
            public Long getCapturedAmount() {
                return capturedAmount;
            }

            @Override
            public Long getRefundedAmount() {
                return refundedAmount;
            }

            @Override
            public Long getGrossAmount() {
                return grossAmount;
            }

            @Override
            public Long getCommissionAmount() {
                return commissionAmount;
            }

            @Override
            public Long getTaxAmount() {
                return taxAmount;
            }

            @Override
            public Long getSellerNetAmount() {
                return sellerNetAmount;
            }

            @Override
            public Long getAvailableBalance() {
                return availableBalance;
            }

            @Override
            public Long getPendingBalance() {
                return pendingBalance;
            }

            @Override
            public Long getCompletedPayoutAmount() {
                return completedPayoutAmount;
            }
        };
    }
}