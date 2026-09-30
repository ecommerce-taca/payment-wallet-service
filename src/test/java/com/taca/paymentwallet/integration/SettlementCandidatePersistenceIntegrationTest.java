package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.settlement.SettlementCandidate;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.infrastructure.persistence.adapter.SettlementCandidatePersistenceAdapter;
import com.taca.paymentwallet.infrastructure.persistence.repository.PaymentAllocationJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(
        disabledWithoutDocker = true
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
class SettlementCandidatePersistenceIntegrationTest {

    private static final UUID SHOP_ID =
            UUID.fromString(
                    "20000000-0000-0000-0000-000000000001"
            );

    private static final UUID WALLET_ID =
            UUID.fromString(
                    "21000000-0000-0000-0000-000000000001"
            );

    private static final UUID FEE_CONFIG_ID =
            UUID.fromString(
                    "10000000-0000-0000-0000-000000000001"
            );

    private static final UUID TAX_CONFIG_ID =
            UUID.fromString(
                    "10000000-0000-0000-0000-000000000002"
            );

    private static final Instant PERIOD_START =
            Instant.parse(
                    "2026-09-01T00:00:00Z"
            );

    private static final Instant PERIOD_END =
            Instant.parse(
                    "2026-09-02T00:00:00Z"
            );

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(
                    "mysql:8.4.0"
            )
                    .withDatabaseName(
                            "payment_wallet_settlement_candidate_test"
                    )
                    .withUsername(
                            "test"
                    )
                    .withPassword(
                            "test"
                    );

    @DynamicPropertySource
    static void configureDatasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                MYSQL::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                MYSQL::getUsername
        );

        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );
    }

    @Autowired
    private PaymentAllocationJpaRepository
            paymentAllocationJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnOnlyUnsettledAllocationInsidePeriod() {
        UUID eligiblePaymentId =
                UUID.randomUUID();

        UUID eligibleAllocationId =
                UUID.randomUUID();

        UUID settledPaymentId =
                UUID.randomUUID();

        UUID settledAllocationId =
                UUID.randomUUID();

        UUID outsidePaymentId =
                UUID.randomUUID();

        UUID outsideAllocationId =
                UUID.randomUUID();

        insertPayment(
                eligiblePaymentId,
                Instant.parse(
                        "2026-09-01T10:00:00Z"
                )
        );

        insertAllocation(
                eligibleAllocationId,
                eligiblePaymentId,
                Instant.parse(
                        "2026-09-01T10:05:00Z"
                )
        );

        insertPayment(
                settledPaymentId,
                Instant.parse(
                        "2026-09-01T11:00:00Z"
                )
        );

        insertAllocation(
                settledAllocationId,
                settledPaymentId,
                Instant.parse(
                        "2026-09-01T11:05:00Z"
                )
        );

        markAllocationAsSettled(
                settledAllocationId
        );

        insertPayment(
                outsidePaymentId,
                Instant.parse(
                        "2026-09-03T10:00:00Z"
                )
        );

        insertAllocation(
                outsideAllocationId,
                outsidePaymentId,
                Instant.parse(
                        "2026-09-03T10:05:00Z"
                )
        );

        SettlementCandidatePersistenceAdapter adapter =
                new SettlementCandidatePersistenceAdapter(
                        paymentAllocationJpaRepository
                );

        List<SettlementCandidate> candidates =
                adapter.findEligibleCandidates(
                        PERIOD_START,
                        PERIOD_END
                );

        assertThat(
                candidates
        ).hasSize(
                1
        );

        SettlementCandidate candidate =
                candidates.getFirst();

        assertThat(
                candidate.paymentAllocationId()
                        .value()
        ).isEqualTo(
                eligibleAllocationId
        );

        assertThat(
                candidate.shopId()
                        .value()
        ).isEqualTo(
                SHOP_ID
        );

        assertThat(
                candidate.walletId()
                        .value()
        ).isEqualTo(
                WALLET_ID
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
                        3_000
                )
        );

        assertThat(
                candidate.sellerNetAmount()
        ).isEqualTo(
                Money.vnd(
                        90_000
                )
        );

        /*
         * Phase 2.3 chỉ là persistence foundation.
         *
         * Settlement hold/refund-window policy vẫn chưa
         * được contract chốt, nên adapter hiện map tạm
         * toàn bộ seller net thành releasable.
         *
         * Adapter này CHƯA được wire vào production
         * RunSettlementUseCase.
         */
        assertThat(
                candidate.releasableAmount()
        ).isEqualTo(
                Money.vnd(
                        90_000
                )
        );

        assertThat(
                candidate.heldAmount()
        ).isEqualTo(
                Money.vnd(0)
        );
    }

    private void insertPayment(
            UUID paymentId,
            Instant createdAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO payments (
                    id,
                    checkout_group_id,
                    buyer_user_id,
                    method,
                    amount,
                    currency,
                    status,
                    captured_amount,
                    refunded_amount,
                    failure_code,
                    expires_at,
                    paid_at,
                    version,
                    created_at,
                    updated_at
                )
                VALUES (
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    'VNPAY',
                    100000,
                    'VND',
                    'SUCCESS',
                    100000,
                    0,
                    NULL,
                    NULL,
                    ?,
                    0,
                    ?,
                    ?
                )
                """,
                paymentId.toString(),
                UUID.randomUUID()
                        .toString(),
                UUID.randomUUID()
                        .toString(),
                Timestamp.from(
                        createdAt
                ),
                Timestamp.from(
                        createdAt
                ),
                Timestamp.from(
                        createdAt
                )
        );
    }

    private void insertAllocation(
            UUID allocationId,
            UUID paymentId,
            Instant createdAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO payment_allocations (
                    id,
                    payment_id,
                    order_id,
                    shop_id,
                    wallet_id,
                    gross_amount,
                    commission_amount,
                    tax_amount,
                    seller_net_amount,
                    currency,
                    fee_config_id,
                    tax_config_id,
                    created_at
                )
                VALUES (
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    100000,
                    7000,
                    3000,
                    90000,
                    'VND',
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    ?
                )
                """,
                allocationId.toString(),
                paymentId.toString(),
                UUID.randomUUID()
                        .toString(),
                SHOP_ID.toString(),
                WALLET_ID.toString(),
                FEE_CONFIG_ID.toString(),
                TAX_CONFIG_ID.toString(),
                Timestamp.from(
                        createdAt
                )
        );
    }

    private void markAllocationAsSettled(
            UUID paymentAllocationId
    ) {
        UUID batchId =
                UUID.randomUUID();

        UUID itemId =
                UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO settlement_batches (
                    id,
                    period_start,
                    period_end,
                    status,
                    shop_count,
                    total_gross,
                    total_commission,
                    total_tax,
                    total_net,
                    total_released,
                    total_held,
                    created_at,
                    closed_at,
                    last_error
                )
                VALUES (
                    UUID_TO_BIN(?),
                    ?,
                    ?,
                    'COMPLETED',
                    1,
                    100000,
                    7000,
                    3000,
                    90000,
                    90000,
                    0,
                    ?,
                    ?,
                    NULL
                )
                """,
                batchId.toString(),
                Timestamp.from(
                        PERIOD_START
                ),
                Timestamp.from(
                        PERIOD_END
                ),
                Timestamp.from(
                        PERIOD_END
                ),
                Timestamp.from(
                        PERIOD_END
                )
        );

        jdbcTemplate.update(
                """
                INSERT INTO settlement_batch_items (
                    id,
                    batch_id,
                    shop_id,
                    wallet_id,
                    gross,
                    commission,
                    tax,
                    net,
                    released_amount,
                    held_amount,
                    status,
                    posting_id,
                    created_at
                )
                VALUES (
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    100000,
                    7000,
                    3000,
                    90000,
                    90000,
                    0,
                    'COMPLETED',
                    NULL,
                    ?
                )
                """,
                itemId.toString(),
                batchId.toString(),
                SHOP_ID.toString(),
                WALLET_ID.toString(),
                Timestamp.from(
                        PERIOD_END
                )
        );

        jdbcTemplate.update(
                """
                INSERT INTO settlement_lines (
                    id,
                    settlement_batch_item_id,
                    payment_allocation_id,
                    released_amount,
                    created_at
                )
                VALUES (
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    UUID_TO_BIN(?),
                    90000,
                    ?
                )
                """,
                UUID.randomUUID()
                        .toString(),
                itemId.toString(),
                paymentAllocationId.toString(),
                Timestamp.from(
                        PERIOD_END
                )
        );
    }
}