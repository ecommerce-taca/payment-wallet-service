package com.taca.paymentwallet.application.query;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetSellerLedgerQueryTest {

    @Test
    void shouldCreateQuery() {
        UUID shopId = UUID.randomUUID();

        GetSellerLedgerQuery query =
                new GetSellerLedgerQuery(
                        shopId,
                        1,
                        20,
                        null,
                        null,
                        "credit"
                );

        assertThat(query.shopId()).isEqualTo(shopId);
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.type()).isEqualTo("CREDIT");
    }

    @Test
    void shouldRejectInvalidPage() {
        assertThatThrownBy(() ->
                new GetSellerLedgerQuery(UUID.randomUUID(), 0, 20, null, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidSize() {
        assertThatThrownBy(() ->
                new GetSellerLedgerQuery(UUID.randomUUID(), 1, 101, null, null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidRange() {
        Instant from = Instant.parse("2026-10-11T10:00:00Z");
        Instant to = Instant.parse("2026-10-11T09:00:00Z");

        assertThatThrownBy(() ->
                new GetSellerLedgerQuery(UUID.randomUUID(), 1, 20, from, to, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNormalizeEntryType() {
        GetSellerLedgerQuery query = new GetSellerLedgerQuery(
                UUID.randomUUID(),
                1,
                20,
                null,
                null,
                " credit "
        );

        assertThat(query.type()).isEqualTo("CREDIT");
    }

    @Test
    void shouldRejectUnsupportedEntryType() {
        assertThatThrownBy(() ->
                new GetSellerLedgerQuery(
                        UUID.randomUUID(),
                        1,
                        20,
                        null,
                        null,
                        "PAYMENT_CAPTURE"
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("type must be DEBIT or CREDIT");
    }

    @Test
    void shouldAllowOpenDateRange() {
        Instant from = Instant.parse("2026-10-01T00:00:00Z");

        GetSellerLedgerQuery query = new GetSellerLedgerQuery(
                UUID.randomUUID(),
                1,
                20,
                from,
                null,
                null
        );

        assertThat(query.from()).isEqualTo(from);
        assertThat(query.to()).isNull();
    }

    @Test
    void shouldRejectEqualRangeBounds() {
        Instant time = Instant.parse("2026-10-01T00:00:00Z");

        assertThatThrownBy(() ->
                new GetSellerLedgerQuery(
                        UUID.randomUUID(),
                        1,
                        20,
                        time,
                        time,
                        null
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }
}