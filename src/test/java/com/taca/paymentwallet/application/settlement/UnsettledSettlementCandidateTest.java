package com.taca.paymentwallet.application.settlement;

import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnsettledSettlementCandidateTest {

    @Test
    void shouldCreateValidUnsettledCandidate() {
        UnsettledSettlementCandidate candidate =
                new UnsettledSettlementCandidate(
                        new PaymentAllocationId(UUID.randomUUID()),
                        new ShopId(UUID.randomUUID()),
                        new WalletId(UUID.randomUUID()),
                        Money.vnd(100_000),
                        Money.vnd(7_000),
                        Money.vnd(1_000),
                        Money.vnd(92_000),
                        Instant.parse("2026-09-01T10:05:00Z")
                );

        assertThat(
                candidate.sellerNetAmount()
        ).isEqualTo(
                Money.vnd(
                        92_000
                )
        );
    }

    @Test
    void shouldRejectUnbalancedFinancialAmounts() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new UnsettledSettlementCandidate(
                                new PaymentAllocationId(
                                        UUID.randomUUID()
                                ),
                                new ShopId(UUID.randomUUID()),
                                new WalletId(UUID.randomUUID()),
                                Money.vnd(100_000),
                                Money.vnd(7_000),
                                Money.vnd(1_000),
                                Money.vnd(90_000),
                                Instant.parse("2026-09-01T10:05:00Z")
                        )
        );
    }
}