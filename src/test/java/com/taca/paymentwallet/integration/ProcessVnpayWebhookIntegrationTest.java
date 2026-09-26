package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.port.out.VnpayWebhookVerifierPort;
import com.taca.paymentwallet.application.result.ProcessVnpayWebhookResult;
import com.taca.paymentwallet.application.result.WebhookProcessingAction;
import com.taca.paymentwallet.application.service.ProcessVnpayWebhookService;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.infrastructure.persistence.adapter.*;
import com.taca.paymentwallet.infrastructure.persistence.entity.*;
import com.taca.paymentwallet.infrastructure.persistence.mapper.*;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import com.taca.paymentwallet.infrastructure.transaction.SpringTransactionAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProcessVnpayWebhookIntegrationTest {

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

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-25T09:30:00Z"
            );

    private static final Instant EXPIRES_AT =
            Instant.parse(
                    "2026-09-25T10:00:00Z"
            );

    private static final String PROVIDER_EVENT_ID =
            "vnpay-e2e-event-001";

    private static final String PROVIDER_TRANSACTION_REF =
            "vnpay-e2e-txn-001";

    /*
     * payment_events.payload_hash VARCHAR(64)
     */
    private static final String PAYLOAD_HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName(
                            "payment_wallet_vnpay_e2e_test"
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
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentOrderJpaRepository paymentOrderJpaRepository;

    @Autowired
    private PaymentAllocationJpaRepository
            paymentAllocationJpaRepository;

    @Autowired
    private WalletJpaRepository walletJpaRepository;

    @Autowired
    private FeeConfigJpaRepository feeConfigJpaRepository;

    @Autowired
    private TaxConfigJpaRepository taxConfigJpaRepository;

    @Autowired
    private LedgerAccountJpaRepository ledgerAccountJpaRepository;

    @Autowired
    private LedgerPostingJpaRepository ledgerPostingJpaRepository;

    @Autowired
    private LedgerEntryJpaRepository ledgerEntryJpaRepository;

    @Autowired
    private SettlementLineJpaRepository settlementLineJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private PaymentEventJpaRepository paymentEventJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldProcessSuccessfulVnpayWebhookEndToEnd() {
        TestIdGenerator idGenerator =
                new TestIdGenerator();

        ProcessVnpayWebhookService service =
                createService(
                        idGenerator
                );

        UUID paymentId =
                UUID.randomUUID();

        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID orderId =
                UUID.randomUUID();

        createPendingVnpayPayment(
                paymentId,
                checkoutGroupId,
                orderId
        );

        WalletJpaEntity walletBefore =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        long availableBefore =
                walletBefore.getAvailableBalance();

        long pendingBefore =
                walletBefore.getPendingBalance();

        ProcessVnpayWebhookResult result =
                service.execute(
                        new ProcessVnpayWebhookCommand(
                                paymentId,
                                PROVIDER_EVENT_ID,
                                PROVIDER_TRANSACTION_REF,
                                "00",
                                "00",
                                100_000,
                                "VND",
                                PAYLOAD_HASH,
                                Map.of(
                                        "vnp_SecureHash",
                                        "signed-value"
                                )
                        )
                );

        assertThat(
                result.paymentId()
        ).isEqualTo(
                paymentId
        );

        assertThat(
                result.paymentStatus()
        ).isEqualTo(
                "SUCCESS"
        );

        assertThat(
                result.action()
        ).isEqualTo(
                WebhookProcessingAction.APPLIED
        );

        assertPaymentSucceeded(
                paymentId
        );

        assertAllocationCreated(
                paymentId,
                orderId,
                idGenerator.allocationId
        );

        assertWalletCredited(
                availableBefore,
                pendingBefore
        );

        assertLedgerPostingCreated(
                paymentId,
                idGenerator.postingId
        );

        assertProviderEventApplied(
                paymentId
        );

        assertOutboxEventCreated(
                paymentId
        );
    }

    private ProcessVnpayWebhookService createService(
            TestIdGenerator idGenerator
    ) {
        ClockPort clockPort =
                () -> NOW;

        TransactionPort transactionPort =
                new SpringTransactionAdapter(
                        transactionManager
                );

        PaymentRepositoryAdapter paymentRepository =
                new PaymentRepositoryAdapter(
                        paymentJpaRepository,
                        paymentOrderJpaRepository,
                        new PaymentPersistenceMapper(),
                        clockPort,
                        new PersistenceUuidGenerator()
                );

        PaymentProviderEventAdapter providerEventAdapter =
                new PaymentProviderEventAdapter(
                        paymentEventJpaRepository,
                        clockPort,
                        new PersistenceUuidGenerator()
                );

        PaymentAllocationRepositoryAdapter allocationRepository =
                new PaymentAllocationRepositoryAdapter(
                        paymentAllocationJpaRepository,
                        new PaymentAllocationPersistenceMapper(),
                        clockPort
                );

        WalletRepositoryAdapter walletRepository =
                new WalletRepositoryAdapter(
                        walletJpaRepository,
                        new WalletPersistenceMapper(),
                        clockPort
                );

        LedgerPostingRepositoryAdapter ledgerRepository =
                new LedgerPostingRepositoryAdapter(
                        ledgerPostingJpaRepository,
                        ledgerEntryJpaRepository,
                        new LedgerPostingPersistenceMapper(),
                        clockPort,
                        new PersistenceUuidGenerator()
                );

        LedgerAccountLookupAdapter accountLookup =
                new LedgerAccountLookupAdapter(
                        ledgerAccountJpaRepository,
                        paymentAllocationJpaRepository,
                        settlementLineJpaRepository
                );

        FeePolicyPersistenceAdapter feePolicy =
                new FeePolicyPersistenceAdapter(
                        feeConfigJpaRepository,
                        taxConfigJpaRepository,
                        clockPort
                );

        OutboxPersistenceAdapter outbox =
                new OutboxPersistenceAdapter(
                        outboxEventJpaRepository,
                        new ObjectMapper()
                );

        VnpayWebhookVerifierPort verifier =
                command -> {
                    /*
                     * Signature verification itself is not
                     * the target of this persistence E2E test.
                     */
                };

        return new ProcessVnpayWebhookService(
                paymentRepository,
                providerEventAdapter,
                allocationRepository,
                walletRepository,
                ledgerRepository,
                accountLookup,
                feePolicy,
                verifier,
                idGenerator,
                clockPort,
                outbox,
                transactionPort,
                new AllocationCalculator(),
                new LedgerPostingFactory()
        );
    }

    private void createPendingVnpayPayment(
            UUID paymentId,
            UUID checkoutGroupId,
            UUID orderId
    ) {
        Payment payment =
                Payment.create(
                        new PaymentId(
                                paymentId
                        ),
                        new CheckoutGroupId(
                                checkoutGroupId
                        ),
                        new BuyerUserId(
                                UUID.randomUUID()
                        ),
                        PaymentMethod.VNPAY,
                        Money.vnd(
                                100_000
                        ),
                        List.of(
                                new PaymentOrder(
                                        new OrderId(
                                                orderId
                                        ),
                                        new ShopId(
                                                SHOP_ID
                                        ),
                                        Money.vnd(
                                                100_000
                                        )
                                )
                        ),
                        EXPIRES_AT
                );

        ClockPort clockPort =
                () -> NOW;

        PaymentRepositoryAdapter paymentRepository =
                new PaymentRepositoryAdapter(
                        paymentJpaRepository,
                        paymentOrderJpaRepository,
                        new PaymentPersistenceMapper(),
                        clockPort,
                        new PersistenceUuidGenerator()
                );

        TransactionPort transactionPort =
                new SpringTransactionAdapter(
                        transactionManager
                );

        transactionPort.execute(
                () ->
                        paymentRepository.save(
                                payment
                        )
        );
    }

    private void assertPaymentSucceeded(
            UUID paymentId
    ) {
        PaymentJpaEntity payment =
                paymentJpaRepository
                        .findById(
                                paymentId
                        )
                        .orElseThrow();

        assertThat(
                payment.getMethod()
        ).isEqualTo(
                "VNPAY"
        );

        assertThat(
                payment.getStatus()
        ).isEqualTo(
                "SUCCESS"
        );

        assertThat(
                payment.getCapturedAmount()
        ).isEqualTo(
                100_000L
        );

        assertThat(
                payment.getRefundedAmount()
        ).isZero();

        assertThat(
                payment.getFailureCode()
        ).isNull();

        assertThat(
                payment.getPaidAt()
        ).isEqualTo(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                NOW
                        )
        );
    }

    private void assertAllocationCreated(
            UUID paymentId,
            UUID orderId,
            UUID allocationId
    ) {
        List<PaymentAllocationJpaEntity> allocations =
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId
                        );

        assertThat(
                allocations
        ).hasSize(1);

        PaymentAllocationJpaEntity allocation =
                allocations.getFirst();

        assertThat(
                allocation.getId()
        ).isEqualTo(
                allocationId
        );

        assertThat(
                allocation.getPaymentId()
        ).isEqualTo(
                paymentId
        );

        assertThat(
                allocation.getOrderId()
        ).isEqualTo(
                orderId
        );

        assertThat(
                allocation.getShopId()
        ).isEqualTo(
                SHOP_ID
        );

        assertThat(
                allocation.getWalletId()
        ).isEqualTo(
                WALLET_ID
        );

        assertThat(
                allocation.getGrossAmount()
        ).isEqualTo(
                100_000L
        );

        /*
         * V014:
         * commission = 7%
         * tax        = 3%
         */
        assertThat(
                allocation.getCommissionAmount()
        ).isEqualTo(
                7_000L
        );

        assertThat(
                allocation.getTaxAmount()
        ).isEqualTo(
                3_000L
        );

        assertThat(
                allocation.getSellerNetAmount()
        ).isEqualTo(
                90_000L
        );

        assertThat(
                allocation.getFeeConfigId()
        ).isEqualTo(
                FEE_CONFIG_ID
        );

        assertThat(
                allocation.getTaxConfigId()
        ).isEqualTo(
                TAX_CONFIG_ID
        );
    }

    private void assertWalletCredited(
            long availableBefore,
            long pendingBefore
    ) {
        WalletJpaEntity wallet =
                walletJpaRepository
                        .findById(
                                WALLET_ID
                        )
                        .orElseThrow();

        assertThat(
                wallet.getAvailableBalance()
        ).isEqualTo(
                availableBefore
        );

        assertThat(
                wallet.getPendingBalance()
        ).isEqualTo(
                pendingBefore
                        + 90_000L
        );
    }

    private void assertLedgerPostingCreated(
            UUID paymentId,
            UUID postingId
    ) {
        LedgerPostingJpaEntity posting =
                ledgerPostingJpaRepository
                        .findById(
                                postingId
                        )
                        .orElseThrow();

        assertThat(
                posting.getPostingType()
        ).isEqualTo(
                "PAYMENT_CAPTURE"
        );

        assertThat(
                posting.getBusinessKey()
        ).isEqualTo(
                "PAYMENT_CAPTURE:"
                        + paymentId
        );

        assertThat(
                posting.getReferenceType()
        ).isEqualTo(
                "PAYMENT"
        );

        assertThat(
                posting.getReferenceId()
        ).isEqualTo(
                paymentId
        );

        List<LedgerEntryJpaEntity> entries =
                ledgerEntryJpaRepository
                        .findByPostingId(
                                postingId
                        );

        assertThat(
                entries
        ).hasSize(4);

        long debit =
                entries.stream()
                        .filter(entry ->
                                "DEBIT".equals(
                                        entry.getEntryType()
                                )
                        )
                        .mapToLong(
                                LedgerEntryJpaEntity::getAmount
                        )
                        .sum();

        long credit =
                entries.stream()
                        .filter(entry ->
                                "CREDIT".equals(
                                        entry.getEntryType()
                                )
                        )
                        .mapToLong(
                                LedgerEntryJpaEntity::getAmount
                        )
                        .sum();

        assertThat(
                debit
        ).isEqualTo(
                100_000L
        );

        assertThat(
                credit
        ).isEqualTo(
                100_000L
        );
    }

    private void assertProviderEventApplied(
            UUID paymentId
    ) {
        PaymentEventJpaEntity event =
                paymentEventJpaRepository
                        .findByProviderAndProviderEventId(
                                "VNPAY",
                                PROVIDER_EVENT_ID
                        )
                        .orElseThrow();

        assertThat(
                event.getPaymentId()
        ).isEqualTo(
                paymentId
        );

        assertThat(
                event.getProvider()
        ).isEqualTo(
                "VNPAY"
        );

        assertThat(
                event.getProviderEventId()
        ).isEqualTo(
                PROVIDER_EVENT_ID
        );

        assertThat(
                event.getProviderTransactionRef()
        ).isEqualTo(
                PROVIDER_TRANSACTION_REF
        );

        assertThat(
                event.getProviderResponseCode()
        ).isEqualTo(
                "00"
        );

        assertThat(
                event.getProviderTransactionStatus()
        ).isEqualTo(
                "00"
        );

        assertThat(
                event.getAmount()
        ).isEqualTo(
                100_000L
        );

        assertThat(
                event.getCurrency()
        ).isEqualTo(
                "VND"
        );

        assertThat(
                event.getPayloadHash()
        ).isEqualTo(
                PAYLOAD_HASH
        );

        assertThat(
                event.getStatus()
        ).isEqualTo(
                "APPLIED"
        );

        assertThat(
                event.getAppliedAt()
        ).isEqualTo(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                NOW
                        )
        );

        assertThat(
                event.getFailureCode()
        ).isNull();
    }

    private void assertOutboxEventCreated(
            UUID paymentId
    ) {
        List<OutboxEventJpaEntity> events =
                outboxEventJpaRepository
                        .findByPublishedAtIsNullOrderByOccurredAtAsc(
                                PageRequest.of(
                                        0,
                                        100
                                )
                        )
                        .stream()
                        .filter(event ->
                                paymentId.equals(
                                        event.getAggregateId()
                                )
                        )
                        .toList();

        assertThat(
                events
        ).hasSize(1);

        OutboxEventJpaEntity event =
                events.getFirst();

        assertThat(
                event.getAggregateType()
        ).isEqualTo(
                "PAYMENT"
        );

        assertThat(
                event.getEventType()
        ).isEqualTo(
                "payment.succeeded"
        );

        assertThat(
                event.getPublishedAt()
        ).isNull();

        assertThat(
                event.getRetryCount()
        ).isZero();

        assertThat(
                event.getPayload()
        ).contains(
                paymentId.toString()
        );
    }

    private static final class TestIdGenerator
            implements IdGeneratorPort {

        private final UUID allocationId =
                UUID.randomUUID();

        private final UUID postingId =
                UUID.randomUUID();

        @Override
        public PaymentId nextPaymentId() {
            return new PaymentId(
                    UUID.randomUUID()
            );
        }

        @Override
        public PaymentAllocationId nextPaymentAllocationId() {
            return new PaymentAllocationId(
                    allocationId
            );
        }

        @Override
        public WalletId nextWalletId() {
            return new WalletId(
                    UUID.randomUUID()
            );
        }

        @Override
        public LedgerAccountId nextLedgerAccountId() {
            return new LedgerAccountId(
                    UUID.randomUUID()
            );
        }

        @Override
        public LedgerPostingId nextLedgerPostingId() {
            return new LedgerPostingId(
                    postingId
            );
        }

        @Override
        public RefundId nextRefundId() {
            return new RefundId(
                    UUID.randomUUID()
            );
        }

        @Override
        public PayoutId nextPayoutId() {
            return new PayoutId(
                    UUID.randomUUID()
            );
        }

        @Override
        public SettlementBatchId nextSettlementBatchId() {
            return new SettlementBatchId(
                    UUID.randomUUID()
            );
        }

        @Override
        public SettlementBatchItemId nextSettlementBatchItemId() {
            return new SettlementBatchItemId(
                    UUID.randomUUID()
            );
        }

        @Override
        public SettlementLineId nextSettlementLineId() {
            return new SettlementLineId(
                    UUID.randomUUID()
            );
        }
    }
}