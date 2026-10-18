package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.out.SellerRevenueDailyView;
import com.taca.paymentwallet.application.port.out.SellerRevenueReadPort;
import com.taca.paymentwallet.application.query.GetSellerRevenueQuery;
import com.taca.paymentwallet.application.query.RevenueGranularity;
import com.taca.paymentwallet.application.result.GetSellerRevenueResult;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetSellerRevenueServiceTest {

    @Test
    void shouldBuildRevenueSummary() {
        UUID shopId = UUID.randomUUID();

        SellerRevenueReadPort port = (
                requestedShopId,
                from,
                to
        ) -> List.of(
                row(LocalDate.of(2026, 8, 1), 100_000, 7_000, 1_000, 92_000, 10_000, 1),
                row(LocalDate.of(2026, 8, 2), 200_000, 14_000, 2_000, 184_000, 0, 2)
        );

        GetSellerRevenueService service = new GetSellerRevenueService(port);

        GetSellerRevenueResult result = service.execute(
                new GetSellerRevenueQuery(
                        shopId,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        RevenueGranularity.DAY
                )
        );

        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.summary().gross()).isEqualTo(300_000);
        assertThat(result.summary().commission()).isEqualTo(21_000);
        assertThat(result.summary().tax()).isEqualTo(3_000);
        assertThat(result.summary().net()).isEqualTo(276_000);
        assertThat(result.summary().refunded()).isEqualTo(10_000);
        assertThat(result.summary().orderCount()).isEqualTo(3);
        assertThat(result.buckets()).hasSize(2);
    }

    @Test
    void shouldGroupRevenueByWeek() {
        SellerRevenueReadPort port = (
                shopId,
                from,
                to
        ) -> List.of(
                row(LocalDate.of(2026, 8, 3), 100_000, 7_000, 1_000, 92_000, 0, 1),
                row(LocalDate.of(2026, 8, 5), 200_000, 14_000, 2_000, 184_000, 0, 2),
                row(LocalDate.of(2026, 8, 10), 300_000, 21_000, 3_000, 276_000, 0, 3)
        );

        GetSellerRevenueService service = new GetSellerRevenueService(port);

        GetSellerRevenueResult result = service.execute(
                new GetSellerRevenueQuery(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        RevenueGranularity.WEEK
                )
        );

        assertThat(result.buckets()).hasSize(2);
        assertThat(result.buckets().get(0).period()).isEqualTo("2026-08-03");
        assertThat(result.buckets().get(0).gross()).isEqualTo(300_000);
        assertThat(result.buckets().get(0).orderCount()).isEqualTo(3);
        assertThat(result.buckets().get(1).period()).isEqualTo("2026-08-10");
        assertThat(result.buckets().get(1).gross()).isEqualTo(300_000);
    }

    @Test
    void shouldGroupRevenueByMonth() {
        SellerRevenueReadPort port = (
                shopId,
                from,
                to
        ) -> List.of(
                row(LocalDate.of(2026, 8, 20), 100_000, 7_000, 1_000, 92_000, 0, 1),
                row(LocalDate.of(2026, 9, 2), 200_000, 14_000, 2_000, 184_000, 0, 1)
        );

        GetSellerRevenueService service = new GetSellerRevenueService(port);

        GetSellerRevenueResult result = service.execute(
                new GetSellerRevenueQuery(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 9, 30),
                        RevenueGranularity.MONTH
                )
        );

        assertThat(result.buckets()).hasSize(2);
        assertThat(result.buckets().get(0).period()).isEqualTo("2026-08-01");
        assertThat(result.buckets().get(1).period()).isEqualTo("2026-09-01");
    }

    @Test
    void shouldReturnZeroSummaryForEmptyRevenue() {
        SellerRevenueReadPort port = (
                shopId,
                from,
                to
        ) -> List.of();

        GetSellerRevenueService service = new GetSellerRevenueService(port);

        GetSellerRevenueResult result = service.execute(
                new GetSellerRevenueQuery(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        RevenueGranularity.DAY
                )
        );

        assertThat(result.summary().gross()).isZero();
        assertThat(result.summary().commission()).isZero();
        assertThat(result.summary().tax()).isZero();
        assertThat(result.summary().net()).isZero();
        assertThat(result.summary().refunded()).isZero();
        assertThat(result.summary().orderCount()).isZero();
        assertThat(result.buckets()).isEmpty();
    }

    @Test
    void shouldIncludeRefundOnlyDayInSummary() {
        SellerRevenueReadPort port = (
                shopId,
                from,
                to
        ) -> List.of(
                row(
                        LocalDate.of(2026, 8, 5),
                        0,
                        0,
                        0,
                        0,
                        25_000,
                        0
                )
        );

        GetSellerRevenueService service = new GetSellerRevenueService(port);

        GetSellerRevenueResult result = service.execute(
                new GetSellerRevenueQuery(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        RevenueGranularity.DAY
                )
        );

        assertThat(result.summary().gross()).isZero();
        assertThat(result.summary().refunded()).isEqualTo(25_000);
        assertThat(result.summary().orderCount()).isZero();

        assertThat(result.buckets()).hasSize(1);
        assertThat(result.buckets().getFirst().period()).isEqualTo("2026-08-05");
    }

    @Test
    void shouldSumRefundedRevenueAcrossDays() {
        SellerRevenueReadPort port = (
                shopId,
                from,
                to
        ) -> List.of(
                row(
                        LocalDate.of(2026, 8, 5),
                        100_000,
                        7_000,
                        1_000,
                        92_000,
                        10_000,
                        1
                ),
                row(
                        LocalDate.of(2026, 8, 10),
                        200_000,
                        14_000,
                        2_000,
                        184_000,
                        30_000,
                        2
                )
        );

        GetSellerRevenueService service = new GetSellerRevenueService(port);

        GetSellerRevenueResult result = service.execute(
                new GetSellerRevenueQuery(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        RevenueGranularity.MONTH
                )
        );

        assertThat(result.summary().refunded()).isEqualTo(40_000);
        assertThat(result.summary().gross()).isEqualTo(300_000);
        assertThat(result.buckets()).hasSize(1);
    }

    private SellerRevenueDailyView row(
            LocalDate date,
            long gross,
            long commission,
            long tax,
            long net,
            long refunded,
            long orderCount
    ) {
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