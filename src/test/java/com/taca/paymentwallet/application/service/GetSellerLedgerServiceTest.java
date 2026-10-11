package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.out.SellerLedgerEntryView;
import com.taca.paymentwallet.application.port.out.SellerLedgerPage;
import com.taca.paymentwallet.application.port.out.SellerLedgerReadPort;
import com.taca.paymentwallet.application.query.GetSellerLedgerQuery;
import com.taca.paymentwallet.application.result.GetSellerLedgerResult;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetSellerLedgerServiceTest {

    @Test
    void shouldReturnSellerLedger() {
        UUID shopId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        UUID postingId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-11T05:00:00Z");

        SellerLedgerReadPort port = (
                requestedShopId,
                page,
                size,
                from,
                to,
                type
        ) -> new SellerLedgerPage(
                List.of(
                        new SellerLedgerEntryView(
                                entryId,
                                postingId,
                                "CREDIT",
                                92_000,
                                92_000,
                                "PAYMENT",
                                referenceId,
                                null,
                                createdAt
                        )
                ),
                1,
                1
        );

        GetSellerLedgerService service = new GetSellerLedgerService(port);

        GetSellerLedgerResult result = service.execute(
                new GetSellerLedgerQuery(shopId, 1, 20, null, null, null)
        );

        assertThat(result.entries()).hasSize(1);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);

        var entry = result.entries().getFirst();

        assertThat(entry.entryId()).isEqualTo(entryId);
        assertThat(entry.postingId()).isEqualTo(postingId);
        assertThat(entry.entryType()).isEqualTo("CREDIT");
        assertThat(entry.amount()).isEqualTo(92_000);
        assertThat(entry.balanceAfter()).isEqualTo(92_000);
        assertThat(entry.referenceType()).isEqualTo("PAYMENT");
        assertThat(entry.referenceId()).isEqualTo(referenceId);
        assertThat(entry.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldRejectNullQuery() {
        SellerLedgerReadPort port = mockPort();
        GetSellerLedgerService service = new GetSellerLedgerService(port);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    private SellerLedgerReadPort mockPort() {
        return new SellerLedgerReadPort() {
            @Override
            public SellerLedgerPage findByShop(
                    ShopId shopId,
                    int page,
                    int size,
                    Instant from,
                    Instant to,
                    String type
            ) {
                return new SellerLedgerPage(List.of(), 0, 0);
            }
        };
    }
}