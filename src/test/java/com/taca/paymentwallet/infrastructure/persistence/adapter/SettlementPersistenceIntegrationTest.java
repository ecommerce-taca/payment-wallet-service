package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.settlement.SettlementBatch;
import com.taca.paymentwallet.domain.settlement.SettlementBatchItem;
import com.taca.paymentwallet.domain.settlement.SettlementLine;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.infrastructure.persistence.entity.*;
import com.taca.paymentwallet.infrastructure.persistence.mapper.SettlementPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class SettlementPersistenceIntegrationTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-25T08:00:00Z"
            );

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName(
                            "payment_wallet_settlement_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

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
    private SettlementBatchJpaRepository batchRepository;

    @Autowired
    private SettlementBatchItemJpaRepository itemRepository;

    @Autowired
    private SettlementLineJpaRepository lineRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private WalletJpaRepository walletRepository;

    @Autowired
    private FeeConfigJpaRepository feeRepository;

    @Autowired
    private TaxConfigJpaRepository taxRepository;

    @Autowired
    private PaymentAllocationJpaRepository allocationRepository;

    @Autowired
    private LedgerAccountJpaRepository ledgerAccountRepository;

    @Autowired
    private LedgerPostingJpaRepository postingRepository;

    @Test
    void shouldPersistCompletedSettlementHierarchy() {
        Fixture fixture =
                createFinancialFixture();

        SettlementBatch batch =
                completedBatch(
                        fixture.wallet().getId(),
                        fixture.wallet().getShopId(),
                        fixture.allocation().getId(),
                        fixture.posting().getId(),
                        Instant.parse(
                                "2026-09-01T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-02T00:00:00Z"
                        )
                );

        SettlementRepositoryAdapter adapter =
                new SettlementRepositoryAdapter(
                        batchRepository,
                        itemRepository,
                        lineRepository,
                        new SettlementPersistenceMapper(),
                        fixedClock()
                );

        adapter.save(batch);

        batchRepository.flush();
        itemRepository.flush();
        lineRepository.flush();

        SettlementBatchJpaEntity storedBatch =
                batchRepository
                        .findById(
                                batch.id().value()
                        )
                        .orElseThrow();

        assertThat(
                storedBatch.getStatus()
        ).isEqualTo("COMPLETED");

        assertThat(
                storedBatch.getShopCount()
        ).isEqualTo(1);

        assertThat(
                storedBatch.getTotalGross()
        ).isEqualTo(100_000L);

        assertThat(
                storedBatch.getTotalCommission()
        ).isEqualTo(7_000L);

        assertThat(
                storedBatch.getTotalTax()
        ).isEqualTo(1_000L);

        assertThat(
                storedBatch.getTotalNet()
        ).isEqualTo(92_000L);

        assertThat(
                storedBatch.getTotalReleased()
        ).isEqualTo(92_000L);

        assertThat(
                storedBatch.getTotalHeld()
        ).isZero();

        assertThat(
                storedBatch.getClosedAt()
        ).isEqualTo(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        8,
                        0
                )
        );

        List<SettlementBatchItemJpaEntity> items =
                itemRepository.findByBatchId(
                        batch.id().value()
                );

        assertThat(items)
                .hasSize(1);

        SettlementBatchItemJpaEntity storedItem =
                items.getFirst();

        assertThat(
                storedItem.getWalletId()
        ).isEqualTo(
                fixture.wallet().getId()
        );

        assertThat(
                storedItem.getShopId()
        ).isEqualTo(
                fixture.wallet().getShopId()
        );

        assertThat(
                storedItem.getStatus()
        ).isEqualTo("COMPLETED");

        assertThat(
                storedItem.getPostingId()
        ).isEqualTo(
                fixture.posting().getId()
        );

        assertThat(
                lineRepository
                        .existsByPaymentAllocationId(
                                fixture.allocation().getId()
                        )
        ).isTrue();
    }

    @Test
    void shouldPersistMultipleItemsAndLinesUnderSameBatch() {
        Fixture first =
                createFinancialFixture();

        Fixture second =
                createFinancialFixture();

        SettlementBatchItem firstItem =
                completedItem(
                        first.wallet().getId(),
                        first.wallet().getShopId(),
                        first.allocation().getId(),
                        first.posting().getId()
                );

        SettlementBatchItem secondItem =
                completedItem(
                        second.wallet().getId(),
                        second.wallet().getShopId(),
                        second.allocation().getId(),
                        second.posting().getId()
                );

        SettlementBatch batch =
                SettlementBatch.create(
                        new SettlementBatchId(
                                UUID.randomUUID()
                        ),
                        Instant.parse(
                                "2026-09-03T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-04T00:00:00Z"
                        ),
                        List.of(
                                firstItem,
                                secondItem
                        )
                );

        batch.markProcessing();
        batch.markCompleted();
        batch.clearDomainEvents();

        SettlementRepositoryAdapter adapter =
                adapter();

        adapter.save(batch);

        batchRepository.flush();
        itemRepository.flush();
        lineRepository.flush();

        assertThat(
                itemRepository.findByBatchId(
                        batch.id().value()
                )
        ).hasSize(2);

        assertThat(
                lineRepository
                        .existsByPaymentAllocationId(
                                first.allocation().getId()
                        )
        ).isTrue();

        assertThat(
                lineRepository
                        .existsByPaymentAllocationId(
                                second.allocation().getId()
                        )
        ).isTrue();

        SettlementBatchJpaEntity stored =
                batchRepository
                        .findById(
                                batch.id().value()
                        )
                        .orElseThrow();

        assertThat(
                stored.getShopCount()
        ).isEqualTo(2);

        assertThat(
                stored.getTotalGross()
        ).isEqualTo(200_000L);

        assertThat(
                stored.getTotalReleased()
        ).isEqualTo(184_000L);
    }

    @Test
    void shouldRejectSecondSettlementForSamePaymentAllocation() {
        Fixture fixture =
                createFinancialFixture();

        SettlementRepositoryAdapter adapter =
                adapter();

        SettlementBatch firstBatch =
                completedBatch(
                        fixture.wallet().getId(),
                        fixture.wallet().getShopId(),
                        fixture.allocation().getId(),
                        fixture.posting().getId(),
                        Instant.parse(
                                "2026-09-05T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-06T00:00:00Z"
                        )
                );

        adapter.save(firstBatch);

        lineRepository.flush();

        /*
         * Batch thứ hai dùng cùng paymentAllocationId
         * nhưng IDs/period hoàn toàn khác.
         */
        SettlementBatch secondBatch =
                completedBatch(
                        fixture.wallet().getId(),
                        fixture.wallet().getShopId(),
                        fixture.allocation().getId(),
                        fixture.posting().getId(),
                        Instant.parse(
                                "2026-09-07T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-08T00:00:00Z"
                        )
                );

        assertThatThrownBy(
                () -> adapter.save(secondBatch)
        ).isInstanceOf(
                PaymentAllocationAlreadySettledException.class
        );

        /*
         * Fail-fast xảy ra trước khi batch thứ hai được save.
         */
        assertThat(
                batchRepository
                        .findById(
                                secondBatch.id().value()
                        )
        ).isEmpty();
    }

    @Test
    void databaseShouldEnforceUniquePaymentAllocationSettlement() {
        Fixture fixture =
                createFinancialFixture();

        SettlementRepositoryAdapter adapter =
                adapter();

        SettlementBatch first =
                completedBatch(
                        fixture.wallet().getId(),
                        fixture.wallet().getShopId(),
                        fixture.allocation().getId(),
                        fixture.posting().getId(),
                        Instant.parse(
                                "2026-09-09T00:00:00Z"
                        ),
                        Instant.parse(
                                "2026-09-10T00:00:00Z"
                        )
                );

        adapter.save(first);

        lineRepository.flush();

        /*
         * Bypass adapter fail-fast để kiểm tra
         * chính DB UNIQUE constraint.
         */
        SettlementBatchId secondBatchId =
                new SettlementBatchId(
                        UUID.randomUUID()
                );

        SettlementBatchItemId secondItemId =
                new SettlementBatchItemId(
                        UUID.randomUUID()
                );

        SettlementLineJpaEntity duplicateLine =
                new SettlementLineJpaEntity();

        duplicateLine.setId(
                UUID.randomUUID()
        );

        /*
         * Để test UNIQUE(payment_allocation_id),
         * trước hết phải có valid batch + item FK.
         */
        SettlementBatchJpaEntity batchEntity =
                new SettlementBatchJpaEntity();

        batchEntity.setId(
                secondBatchId.value()
        );

        batchEntity.setPeriodStart(
                LocalDateTime.of(
                        2026,
                        9,
                        11,
                        0,
                        0
                )
        );

        batchEntity.setPeriodEnd(
                LocalDateTime.of(
                        2026,
                        9,
                        12,
                        0,
                        0
                )
        );

        batchEntity.setStatus("PROCESSING");
        batchEntity.setShopCount(1);
        batchEntity.setTotalGross(100_000L);
        batchEntity.setTotalCommission(7_000L);
        batchEntity.setTotalTax(1_000L);
        batchEntity.setTotalNet(92_000L);
        batchEntity.setTotalReleased(92_000L);
        batchEntity.setTotalHeld(0L);
        batchEntity.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        8,
                        0
                )
        );

        batchRepository.saveAndFlush(
                batchEntity
        );

        SettlementBatchItemJpaEntity itemEntity =
                new SettlementBatchItemJpaEntity();

        itemEntity.setId(
                secondItemId.value()
        );

        itemEntity.setBatchId(
                secondBatchId.value()
        );

        itemEntity.setShopId(
                fixture.wallet().getShopId()
        );

        itemEntity.setWalletId(
                fixture.wallet().getId()
        );

        itemEntity.setGross(100_000L);
        itemEntity.setCommission(7_000L);
        itemEntity.setTax(1_000L);
        itemEntity.setNet(92_000L);
        itemEntity.setReleasedAmount(92_000L);
        itemEntity.setHeldAmount(0L);
        itemEntity.setStatus("PENDING");
        itemEntity.setPostingId(null);
        itemEntity.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        8,
                        0
                )
        );

        itemRepository.saveAndFlush(
                itemEntity
        );

        duplicateLine.setSettlementBatchItemId(
                secondItemId.value()
        );

        duplicateLine.setPaymentAllocationId(
                fixture.allocation().getId()
        );

        duplicateLine.setReleasedAmount(
                92_000L
        );

        duplicateLine.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        8,
                        0
                )
        );

        assertThatThrownBy(
                () ->
                        lineRepository
                                .saveAndFlush(
                                        duplicateLine
                                )
        ).isInstanceOf(
                org.springframework.dao.DataIntegrityViolationException.class
        );
    }

    private SettlementRepositoryAdapter adapter() {
        return new SettlementRepositoryAdapter(
                batchRepository,
                itemRepository,
                lineRepository,
                new SettlementPersistenceMapper(),
                fixedClock()
        );
    }

    private ClockPort fixedClock() {
        return () -> NOW;
    }

    private SettlementBatch completedBatch(
            UUID walletId,
            UUID shopId,
            UUID allocationId,
            UUID postingId,
            Instant periodStart,
            Instant periodEnd
    ) {
        SettlementBatchItem item =
                completedItem(
                        walletId,
                        shopId,
                        allocationId,
                        postingId
                );

        SettlementBatch batch =
                SettlementBatch.create(
                        new SettlementBatchId(
                                UUID.randomUUID()
                        ),
                        periodStart,
                        periodEnd,
                        List.of(item)
                );

        batch.markProcessing();
        batch.markCompleted();
        batch.clearDomainEvents();

        return batch;
    }

    private SettlementBatchItem completedItem(
            UUID walletId,
            UUID shopId,
            UUID allocationId,
            UUID postingId
    ) {
        SettlementLine line =
                new SettlementLine(
                        new SettlementLineId(
                                UUID.randomUUID()
                        ),
                        new PaymentAllocationId(
                                allocationId
                        ),
                        Money.vnd(92_000)
                );

        SettlementBatchItem item =
                SettlementBatchItem.create(
                        new SettlementBatchItemId(
                                UUID.randomUUID()
                        ),
                        new ShopId(shopId),
                        new WalletId(walletId),
                        Money.vnd(100_000),
                        Money.vnd(7_000),
                        Money.vnd(1_000),
                        Money.vnd(92_000),
                        Money.vnd(92_000),
                        Money.vnd(0),
                        List.of(line)
                );

        item.markCompleted(
                new LedgerPostingId(
                        postingId
                )
        );

        return item;
    }

    private Fixture createFinancialFixture() {
        LocalDateTime now =
                LocalDateTime.of(
                        2026,
                        9,
                        25,
                        8,
                        0
                );

        UUID settlementReferenceId = UUID.randomUUID();

        PaymentJpaEntity payment =
                new PaymentJpaEntity();

        payment.setId(
                UUID.randomUUID()
        );

        payment.setCheckoutGroupId(
                UUID.randomUUID()
        );

        payment.setBuyerUserId(
                UUID.randomUUID()
        );

        payment.setMethod("VNPAY");
        payment.setAmount(100_000L);
        payment.setCurrency("VND");
        payment.setStatus("SUCCESS");
        payment.setCapturedAmount(100_000L);
        payment.setRefundedAmount(0L);
        payment.setExpiresAt(
                now.plusMinutes(15)
        );
        payment.setPaidAt(now);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        paymentRepository.saveAndFlush(
                payment
        );

        WalletJpaEntity wallet =
                new WalletJpaEntity();

        wallet.setId(
                UUID.randomUUID()
        );

        wallet.setShopId(
                UUID.randomUUID()
        );

        wallet.setCurrency("VND");
        wallet.setAvailableBalance(0L);
        wallet.setPendingBalance(92_000L);
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(now);
        wallet.setUpdatedAt(now);

        walletRepository.saveAndFlush(
                wallet
        );

        FeeConfigJpaEntity fee =
                new FeeConfigJpaEntity();

        fee.setId(
                UUID.randomUUID()
        );

        fee.setScope("PLATFORM");
        fee.setCategoryId(null);
        fee.setRateBps(700);
        fee.setEffectiveFrom(
                now.minusDays(1)
        );
        fee.setNote(
                "Settlement integration fixture"
        );
        fee.setCreatedBy(
                UUID.randomUUID()
        );
        fee.setCreatedAt(now);

        feeRepository.saveAndFlush(
                fee
        );

        TaxConfigJpaEntity tax =
                new TaxConfigJpaEntity();

        tax.setId(
                UUID.randomUUID()
        );

        tax.setScope("PLATFORM");
        tax.setCategoryId(null);
        tax.setRateBps(100);
        tax.setEffectiveFrom(
                now.minusDays(1)
        );
        tax.setNote(
                "Settlement integration fixture"
        );
        tax.setCreatedBy(
                UUID.randomUUID()
        );
        tax.setCreatedAt(now);

        taxRepository.saveAndFlush(
                tax
        );

        PaymentAllocationJpaEntity allocation =
                new PaymentAllocationJpaEntity();

        allocation.setId(
                UUID.randomUUID()
        );

        allocation.setPaymentId(
                payment.getId()
        );

        allocation.setOrderId(
                UUID.randomUUID()
        );

        allocation.setShopId(
                wallet.getShopId()
        );

        allocation.setWalletId(
                wallet.getId()
        );

        allocation.setGrossAmount(
                100_000L
        );

        allocation.setCommissionAmount(
                7_000L
        );

        allocation.setTaxAmount(
                1_000L
        );

        allocation.setSellerNetAmount(
                92_000L
        );

        allocation.setCurrency("VND");

        allocation.setFeeConfigId(
                fee.getId()
        );

        allocation.setTaxConfigId(
                tax.getId()
        );

        allocation.setCreatedAt(now);

        allocationRepository.saveAndFlush(
                allocation
        );

        /*
         * SettlementBatchItem COMPLETED yêu cầu posting_id,
         * và DB posting_id có FK tới ledger_postings.
         */
        LedgerPostingJpaEntity posting =
                new LedgerPostingJpaEntity();

        posting.setId(
                UUID.randomUUID()
        );

        posting.setPostingType(
                "SETTLEMENT_RELEASE"
        );

        posting.setBusinessKey(
                "SETTLEMENT_RELEASE:"
                        + UUID.randomUUID()
        );

        posting.setReferenceType(
                "SETTLEMENT"
        );

        posting.setReferenceId(
                settlementReferenceId
        );

        posting.setDescription(
                "Settlement integration fixture"
        );

        posting.setCreatedAt(now);

        postingRepository.saveAndFlush(
                posting
        );

        return new Fixture(
                payment,
                wallet,
                allocation,
                posting
        );
    }

    private record Fixture(
            PaymentJpaEntity payment,
            WalletJpaEntity wallet,
            PaymentAllocationJpaEntity allocation,
            LedgerPostingJpaEntity posting
    ) {
    }
}