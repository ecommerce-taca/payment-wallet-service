package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.in.GetSellerRevenueUseCase;
import com.taca.paymentwallet.application.port.out.SellerRevenueDailyView;
import com.taca.paymentwallet.application.port.out.SellerRevenueReadPort;
import com.taca.paymentwallet.application.query.GetSellerRevenueQuery;
import com.taca.paymentwallet.application.query.RevenueGranularity;
import com.taca.paymentwallet.application.result.*;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class GetSellerRevenueService implements GetSellerRevenueUseCase {

    private static final String CURRENCY = "VND";

    private final SellerRevenueReadPort sellerRevenueReadPort;

    public GetSellerRevenueService(SellerRevenueReadPort sellerRevenueReadPort) {
        this.sellerRevenueReadPort = Objects.requireNonNull(sellerRevenueReadPort);
    }

    @Override
    public GetSellerRevenueResult execute(GetSellerRevenueQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        List<SellerRevenueDailyView> daily = sellerRevenueReadPort.findDailyRevenue(
                new ShopId(query.shopId()),
                query.from(),
                query.to()
        );

        return new GetSellerRevenueResult(
                new GetSellerRevenueRangeResult(
                        query.from(),
                        query.to(),
                        query.granularity().name()
                ),
                CURRENCY,
                summary(daily),
                buckets(daily, query.granularity())
        );
    }

    private GetSellerRevenueSummaryResult summary(List<SellerRevenueDailyView> rows) {
        return new GetSellerRevenueSummaryResult(
                rows.stream().mapToLong(SellerRevenueDailyView::gross).sum(),
                rows.stream().mapToLong(SellerRevenueDailyView::commission).sum(),
                rows.stream().mapToLong(SellerRevenueDailyView::tax).sum(),
                rows.stream().mapToLong(SellerRevenueDailyView::net).sum(),
                rows.stream().mapToLong(SellerRevenueDailyView::refunded).sum(),
                rows.stream().mapToLong(SellerRevenueDailyView::orderCount).sum()
        );
    }

    private List<GetSellerRevenueBucketResult> buckets(
            List<SellerRevenueDailyView> rows,
            RevenueGranularity granularity
    ) {
        Map<LocalDate, MutableBucket> grouped = new TreeMap<>();

        for (SellerRevenueDailyView row : rows) {
            LocalDate period = periodStart(row.date(), granularity);
            grouped.computeIfAbsent(period, ignored -> new MutableBucket()).add(row);
        }

        return grouped.entrySet().stream()
                .map(entry -> entry.getValue().toResult(entry.getKey()))
                .toList();
    }

    private LocalDate periodStart(LocalDate date, RevenueGranularity granularity) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private static final class MutableBucket {

        private long gross;
        private long commission;
        private long tax;
        private long net;
        private long orderCount;

        private void add(SellerRevenueDailyView row) {
            gross += row.gross();
            commission += row.commission();
            tax += row.tax();
            net += row.net();
            orderCount += row.orderCount();
        }

        private GetSellerRevenueBucketResult toResult(LocalDate period) {
            return new GetSellerRevenueBucketResult(
                    period.toString(),
                    gross,
                    commission,
                    tax,
                    net,
                    orderCount
            );
        }
    }
}