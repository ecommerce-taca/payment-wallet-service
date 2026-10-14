package com.taca.paymentwallet.application.query;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetSellerRevenueQueryTest {

    @Test
    void shouldCreateRevenueQuery() {
        UUID shopId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);

        GetSellerRevenueQuery query = new GetSellerRevenueQuery(
                shopId,
                from,
                to,
                RevenueGranularity.DAY
        );

        assertThat(query.shopId()).isEqualTo(shopId);
        assertThat(query.from()).isEqualTo(from);
        assertThat(query.to()).isEqualTo(to);
        assertThat(query.granularity()).isEqualTo(RevenueGranularity.DAY);
    }

    @Test
    void shouldRejectInvalidRange() {
        assertThatThrownBy(() ->
                new GetSellerRevenueQuery(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 8, 1),
                        RevenueGranularity.DAY
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAllow366DayRange() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(365);

        GetSellerRevenueQuery query = new GetSellerRevenueQuery(
                UUID.randomUUID(),
                from,
                to,
                RevenueGranularity.DAY
        );

        assertThat(query.from()).isEqualTo(from);
        assertThat(query.to()).isEqualTo(to);
    }

    @Test
    void shouldRejectRangeLongerThan366Days() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(366);

        assertThatThrownBy(() ->
                new GetSellerRevenueQuery(
                        UUID.randomUUID(),
                        from,
                        to,
                        RevenueGranularity.DAY
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("revenue range must not exceed 366 days");
    }
}