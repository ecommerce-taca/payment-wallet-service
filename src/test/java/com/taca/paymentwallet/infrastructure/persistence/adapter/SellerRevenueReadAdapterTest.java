package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.infrastructure.persistence.projection.SellerRevenueAllocationProjection;
import com.taca.paymentwallet.infrastructure.persistence.projection.SellerRevenueRefundProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.RefundAllocationJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SellerRevenueReadAdapterTest {

    @Test
    void shouldMergeAllocationAndRefundRevenueByDay() {
        PaymentAllocationJpaRepository allocationRepository =
                mock(PaymentAllocationJpaRepository.class);

        RefundAllocationJpaRepository refundRepository =
                mock(RefundAllocationJpaRepository.class);

        SellerRevenueAllocationProjection allocation =
                mock(SellerRevenueAllocationProjection.class);

        SellerRevenueRefundProjection refund =
                mock(SellerRevenueRefundProjection.class);

        LocalDate date = LocalDate.of(2026, 8, 1);

        when(allocation.getDate()).thenReturn(date);
        when(allocation.getGross()).thenReturn(100_000L);
        when(allocation.getCommission()).thenReturn(7_000L);
        when(allocation.getTax()).thenReturn(1_000L);
        when(allocation.getNet()).thenReturn(92_000L);
        when(allocation.getOrderCount()).thenReturn(1L);

        when(refund.getDate()).thenReturn(date);
        when(refund.getRefunded()).thenReturn(10_000L);

        when(allocationRepository.findSellerRevenueByDay(any(), any(), any()))
                .thenReturn(List.of(allocation));

        when(refundRepository.findSellerRefundedRevenueByDay(any(), any(), any()))
                .thenReturn(List.of(refund));

        SellerRevenueReadAdapter adapter = new SellerRevenueReadAdapter(
                allocationRepository,
                refundRepository
        );

        var result = adapter.findDailyRevenue(
                new ShopId(UUID.randomUUID()),
                date,
                date
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().gross()).isEqualTo(100_000);
        assertThat(result.getFirst().refunded()).isEqualTo(10_000);
    }

    @Test
    void shouldKeepRefundOnlyDay() {
        PaymentAllocationJpaRepository allocationRepository =
                mock(PaymentAllocationJpaRepository.class);

        RefundAllocationJpaRepository refundRepository =
                mock(RefundAllocationJpaRepository.class);

        SellerRevenueRefundProjection refund =
                mock(SellerRevenueRefundProjection.class);

        LocalDate date = LocalDate.of(2026, 8, 5);

        when(refund.getDate()).thenReturn(date);
        when(refund.getRefunded()).thenReturn(25_000L);

        when(allocationRepository.findSellerRevenueByDay(any(), any(), any()))
                .thenReturn(List.of());

        when(refundRepository.findSellerRefundedRevenueByDay(any(), any(), any()))
                .thenReturn(List.of(refund));

        SellerRevenueReadAdapter adapter = new SellerRevenueReadAdapter(
                allocationRepository,
                refundRepository
        );

        var result = adapter.findDailyRevenue(
                new ShopId(UUID.randomUUID()),
                date,
                date
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().gross()).isZero();
        assertThat(result.getFirst().refunded()).isEqualTo(25_000);
    }
}