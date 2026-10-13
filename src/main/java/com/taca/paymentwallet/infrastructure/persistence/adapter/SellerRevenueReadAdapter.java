package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.SellerRevenueDailyView;
import com.taca.paymentwallet.application.port.out.SellerRevenueReadPort;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.infrastructure.persistence.projection.SellerRevenueAllocationProjection;
import com.taca.paymentwallet.infrastructure.persistence.projection.SellerRevenueRefundProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.repository.RefundAllocationJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class SellerRevenueReadAdapter implements SellerRevenueReadPort {

    private final PaymentAllocationJpaRepository allocationRepository;
    private final RefundAllocationJpaRepository refundAllocationRepository;

    public SellerRevenueReadAdapter(
            PaymentAllocationJpaRepository allocationRepository,
            RefundAllocationJpaRepository refundAllocationRepository
    ) {
        this.allocationRepository = Objects.requireNonNull(allocationRepository);
        this.refundAllocationRepository = Objects.requireNonNull(refundAllocationRepository);
    }

    @Override
    public List<SellerRevenueDailyView> findDailyRevenue(
            ShopId shopId,
            LocalDate from,
            LocalDate to
    ) {
        Objects.requireNonNull(shopId, "shopId must not be null");
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");

        LocalDateTime fromAt = from.atStartOfDay();
        LocalDateTime toAt = to.plusDays(1).atStartOfDay();

        Map<LocalDate, MutableRevenue> rows = new TreeMap<>();

        for (SellerRevenueAllocationProjection row
                : allocationRepository.findSellerRevenueByDay(shopId.value(), fromAt, toAt)) {
            rows.put(
                    row.getDate(),
                    new MutableRevenue(
                            row.getGross(),
                            row.getCommission(),
                            row.getTax(),
                            row.getNet(),
                            0,
                            row.getOrderCount()
                    )
            );
        }

        for (SellerRevenueRefundProjection row
                : refundAllocationRepository.findSellerRefundedRevenueByDay(
                shopId.value(),
                fromAt,
                toAt
        )) {
            MutableRevenue revenue = rows.computeIfAbsent(
                    row.getDate(),
                    ignored -> new MutableRevenue(0, 0, 0, 0, 0, 0)
            );

            revenue.refunded = row.getRefunded();
        }

        return rows.entrySet().stream()
                .map(entry -> entry.getValue().toView(entry.getKey()))
                .toList();
    }

    private static final class MutableRevenue {

        private final long gross;
        private final long commission;
        private final long tax;
        private final long net;
        private long refunded;
        private final long orderCount;

        private MutableRevenue(
                long gross,
                long commission,
                long tax,
                long net,
                long refunded,
                long orderCount
        ) {
            this.gross = gross;
            this.commission = commission;
            this.tax = tax;
            this.net = net;
            this.refunded = refunded;
            this.orderCount = orderCount;
        }

        private SellerRevenueDailyView toView(LocalDate date) {
            return new SellerRevenueDailyView(
                    date,
                    gross,
                    commission,
                    tax,
                    net,
                    refunded,
                    orderCount
            );
        }
    }
}