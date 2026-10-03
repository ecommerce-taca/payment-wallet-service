package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.command.ProcessCodPaymentCommand;
import com.taca.paymentwallet.application.payment.CodPaymentProcessingAction;
import com.taca.paymentwallet.application.payment.CodPaymentResultStatus;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.IdGeneratorPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.result.ProcessCodPaymentResult;
import com.taca.paymentwallet.application.service.ProcessCodPaymentService;
import com.taca.paymentwallet.domain.finance.AllocationCalculator;
import com.taca.paymentwallet.domain.payment.*;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.domain.wallet.LedgerPostingFactory;
import com.taca.paymentwallet.infrastructure.persistence.adapter.*;
import com.taca.paymentwallet.infrastructure.persistence.entity.*;
import com.taca.paymentwallet.infrastructure.persistence.mapper.*;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import com.taca.paymentwallet.infrastructure.transaction.SpringTransactionAdapter;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProcessCodPaymentIntegrationTest {

    private static final UUID SHOP_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID WALLET_ID =
            UUID.fromString("21000000-0000-0000-0000-000000000001");

    private static final UUID FEE_CONFIG_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID TAX_CONFIG_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000002");

    private static final Instant NOW =
            Instant.parse("2026-09-25T09:30:00Z");

    private static final Instant DELIVERED_AT =
            Instant.parse("2026-09-25T09:25:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName("payment_wallet_cod_e2e_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentOrderJpaRepository paymentOrderJpaRepository;

    @Autowired
    private PaymentAllocationJpaRepository paymentAllocationJpaRepository;

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
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetSeedWallet() {
        WalletJpaEntity wallet = walletJpaRepository.findById(WALLET_ID).orElseThrow();
        wallet.setAvailableBalance(31_000L);
        wallet.setPendingBalance(0L);
        walletJpaRepository.save(wallet);
    }

    @Test
    void shouldProcessDeliveredCodOrderEndToEnd() {
        UUID paymentId = UUID.randomUUID();
        UUID checkoutGroupId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        createCodPayment(paymentId, checkoutGroupId, orderId);

        TestIdGenerator idGenerator = new TestIdGenerator();
        ProcessCodPaymentService service = createService(idGenerator);

        ProcessCodPaymentResult result = service.execute(new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.DELIVERED,
                DELIVERED_AT,
                null
        ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.checkoutGroupId()).isEqualTo(checkoutGroupId);
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.paymentStatus()).isEqualTo("SUCCESS");
        assertThat(result.orderCodStatus()).isEqualTo("CAPTURED");

        assertPaymentSucceeded(paymentId);
        assertOrderCaptured(orderId);
        assertAllocationCreated(paymentId, orderId, idGenerator.allocationId);
        assertWalletCredited();
        assertCodLedgerPostingCreated(paymentId, orderId, idGenerator.postingId);
        assertPaymentSucceededOutboxCreated(paymentId);
    }

    @Test
    void shouldProcessFailedCodOrderEndToEnd() {
        UUID paymentId = UUID.randomUUID();
        UUID checkoutGroupId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        createCodPayment(paymentId, checkoutGroupId, orderId);

        ProcessCodPaymentResult result = createService(new TestIdGenerator())
                .execute(new ProcessCodPaymentCommand(
                        orderId,
                        CodPaymentResultStatus.FAILED,
                        DELIVERED_AT,
                        "SHIPMENT_FAILED"
                ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo("FAILED");
        assertThat(result.orderCodStatus()).isEqualTo("FAILED");

        PaymentJpaEntity payment = paymentJpaRepository.findById(paymentId).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo("FAILED");
        assertThat(payment.getFailureCode()).isEqualTo("SHIPMENT_FAILED");
        assertThat(payment.getCapturedAmount()).isZero();
        assertThat(payment.getPaidAt()).isNull();

        PaymentOrderJpaEntity order =
                paymentOrderJpaRepository.findByOrderId(orderId).orElseThrow();

        assertThat(order.getCodStatus()).isEqualTo("FAILED");
        assertThat(order.getCodFailureCode()).isEqualTo("SHIPMENT_FAILED");
        assertThat(order.getCodProcessedAt())
                .isEqualTo(PersistenceTimeMapper.toLocalDateTime(DELIVERED_AT));

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId)
        ).isEmpty();

        assertThat(
                ledgerPostingJpaRepository.findByBusinessKey(
                        "COD_CAPTURE:" + paymentId + ":" + orderId
                )
        ).isEmpty();

        assertPaymentFailedOutboxCreated(paymentId, "SHIPMENT_FAILED");
    }

    @Test
    void shouldProcessCancelledCodOrderEndToEnd() {
        UUID paymentId = UUID.randomUUID();
        UUID checkoutGroupId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        createCodPayment(paymentId, checkoutGroupId, orderId);

        ProcessCodPaymentResult result = createService(new TestIdGenerator())
                .execute(new ProcessCodPaymentCommand(
                        orderId,
                        CodPaymentResultStatus.CANCELLED,
                        DELIVERED_AT,
                        "ORDER_CANCELLED"
                ));

        assertThat(result.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(result.paymentStatus()).isEqualTo("FAILED");
        assertThat(result.orderCodStatus()).isEqualTo("FAILED");

        PaymentJpaEntity payment = paymentJpaRepository.findById(paymentId).orElseThrow();
        PaymentOrderJpaEntity order =
                paymentOrderJpaRepository.findByOrderId(orderId).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo("FAILED");
        assertThat(payment.getFailureCode()).isEqualTo("ORDER_CANCELLED");
        assertThat(payment.getCapturedAmount()).isZero();

        assertThat(order.getCodStatus()).isEqualTo("FAILED");
        assertThat(order.getCodFailureCode()).isEqualTo("ORDER_CANCELLED");

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId)
        ).isEmpty();

        assertPaymentFailedOutboxCreated(paymentId, "ORDER_CANCELLED");
    }

    @Test
    void shouldIgnoreDuplicateDeliveredCodOrderEndToEnd() {
        UUID paymentId = UUID.randomUUID();
        UUID checkoutGroupId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        createCodPayment(paymentId, checkoutGroupId, orderId);

        ProcessCodPaymentService service = createService(new TestIdGenerator());

        ProcessCodPaymentCommand command = new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.DELIVERED,
                DELIVERED_AT,
                null
        );

        ProcessCodPaymentResult first = service.execute(command);

        long pendingAfterFirst =
                walletJpaRepository.findById(WALLET_ID).orElseThrow().getPendingBalance();

        long outboxAfterFirst =
                countOutboxEventsForPayment(paymentId);

        ProcessCodPaymentResult duplicate = service.execute(command);

        assertThat(first.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(duplicate.action()).isEqualTo(CodPaymentProcessingAction.DUPLICATE);
        assertThat(duplicate.paymentStatus()).isEqualTo("SUCCESS");
        assertThat(duplicate.orderCodStatus()).isEqualTo("CAPTURED");

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId)
        ).hasSize(1);

        assertThat(
                ledgerPostingJpaRepository.findByBusinessKey(
                        "COD_CAPTURE:" + paymentId + ":" + orderId
                )
        ).isPresent();

        assertThat(
                walletJpaRepository.findById(WALLET_ID).orElseThrow().getPendingBalance()
        ).isEqualTo(pendingAfterFirst);

        assertThat(countOutboxEventsForPayment(paymentId))
                .isEqualTo(outboxAfterFirst);
    }

    @Test
    void shouldIgnoreDuplicateFailedCodOrderEndToEnd() {
        UUID paymentId = UUID.randomUUID();
        UUID checkoutGroupId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        createCodPayment(paymentId, checkoutGroupId, orderId);

        ProcessCodPaymentService service = createService(new TestIdGenerator());

        ProcessCodPaymentCommand command = new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.FAILED,
                DELIVERED_AT,
                "SHIPMENT_FAILED"
        );

        ProcessCodPaymentResult first = service.execute(command);
        long outboxAfterFirst = countOutboxEventsForPayment(paymentId);

        ProcessCodPaymentResult duplicate = service.execute(command);

        assertThat(first.action()).isEqualTo(CodPaymentProcessingAction.APPLIED);
        assertThat(duplicate.action()).isEqualTo(CodPaymentProcessingAction.DUPLICATE);
        assertThat(duplicate.paymentStatus()).isEqualTo("FAILED");
        assertThat(duplicate.orderCodStatus()).isEqualTo("FAILED");

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId)
        ).isEmpty();

        assertThat(
                ledgerPostingJpaRepository.findByBusinessKey(
                        "COD_CAPTURE:" + paymentId + ":" + orderId
                )
        ).isEmpty();

        assertThat(countOutboxEventsForPayment(paymentId))
                .isEqualTo(outboxAfterFirst);
    }

    @Test
    void shouldPersistCodOrderStateAcrossReload() {
        UUID paymentId = UUID.randomUUID();
        UUID checkoutGroupId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        createCodPayment(paymentId, checkoutGroupId, orderId);

        createService(new TestIdGenerator()).execute(new ProcessCodPaymentCommand(
                orderId,
                CodPaymentResultStatus.DELIVERED,
                DELIVERED_AT,
                null
        ));

        TransactionPort transactionPort = new SpringTransactionAdapter(transactionManager);

        Payment reloaded = transactionPort.execute(() ->
                createPaymentRepository()
                        .findByOrderIdForUpdate(new OrderId(orderId))
                        .orElseThrow()
        );

        assertThat(reloaded.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(reloaded.capturedAmount()).isEqualTo(Money.vnd(100_000));

        PaymentOrder order = reloaded.order(new OrderId(orderId));

        assertThat(order.codStatus()).isEqualTo(PaymentOrderCodStatus.CAPTURED);
        assertThat(order.codProcessedAt()).isEqualTo(DELIVERED_AT);
        assertThat(order.codFailureCode()).isNull();
    }

    private ProcessCodPaymentService createService(TestIdGenerator idGenerator) {
        ClockPort clockPort = () -> NOW;
        TransactionPort transactionPort = new SpringTransactionAdapter(transactionManager);

        WalletRepositoryAdapter walletRepository = new WalletRepositoryAdapter(
                walletJpaRepository,
                new WalletPersistenceMapper(),
                clockPort
        );

        PaymentAllocationRepositoryAdapter allocationRepository =
                new PaymentAllocationRepositoryAdapter(
                        paymentAllocationJpaRepository,
                        new PaymentAllocationPersistenceMapper(),
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

        return new ProcessCodPaymentService(
                createPaymentRepository(),
                allocationRepository,
                walletRepository,
                ledgerRepository,
                accountLookup,
                feePolicy,
                idGenerator,
                outbox,
                transactionPort,
                new AllocationCalculator(),
                new LedgerPostingFactory()
        );
    }

    private PaymentRepositoryAdapter createPaymentRepository() {
        return new PaymentRepositoryAdapter(
                paymentJpaRepository,
                paymentOrderJpaRepository,
                new PaymentPersistenceMapper(),
                () -> NOW,
                new PersistenceUuidGenerator()
        );
    }

    private void createCodPayment(UUID paymentId, UUID checkoutGroupId, UUID orderId) {
        Payment payment = Payment.create(
                new PaymentId(paymentId),
                new CheckoutGroupId(checkoutGroupId),
                new BuyerUserId(UUID.randomUUID()),
                PaymentMethod.COD,
                Money.vnd(100_000),
                List.of(new PaymentOrder(
                        new OrderId(orderId),
                        new ShopId(SHOP_ID),
                        Money.vnd(100_000)
                ))
        );

        payment.clearDomainEvents();

        TransactionPort transactionPort = new SpringTransactionAdapter(transactionManager);
        transactionPort.execute(() -> createPaymentRepository().save(payment));
    }

    private void assertPaymentSucceeded(UUID paymentId) {
        PaymentJpaEntity payment = paymentJpaRepository.findById(paymentId).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getCapturedAmount()).isEqualTo(100_000L);
        assertThat(payment.getRefundedAmount()).isZero();
        assertThat(payment.getFailureCode()).isNull();
        assertThat(payment.getPaidAt())
                .isEqualTo(PersistenceTimeMapper.toLocalDateTime(DELIVERED_AT));
    }

    private void assertOrderCaptured(UUID orderId) {
        PaymentOrderJpaEntity order =
                paymentOrderJpaRepository.findByOrderId(orderId).orElseThrow();

        assertThat(order.getCodStatus()).isEqualTo("CAPTURED");
        assertThat(order.getCodFailureCode()).isNull();
        assertThat(order.getCodProcessedAt())
                .isEqualTo(PersistenceTimeMapper.toLocalDateTime(DELIVERED_AT));
    }

    private void assertAllocationCreated(
            UUID paymentId,
            UUID orderId,
            UUID allocationId
    ) {
        List<PaymentAllocationJpaEntity> allocations =
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(paymentId);

        assertThat(allocations).hasSize(1);

        PaymentAllocationJpaEntity allocation = allocations.getFirst();

        assertThat(allocation.getId()).isEqualTo(allocationId);
        assertThat(allocation.getPaymentId()).isEqualTo(paymentId);
        assertThat(allocation.getOrderId()).isEqualTo(orderId);
        assertThat(allocation.getShopId()).isEqualTo(SHOP_ID);
        assertThat(allocation.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(allocation.getGrossAmount()).isEqualTo(100_000L);
        assertThat(allocation.getCommissionAmount()).isEqualTo(7_000L);
        assertThat(allocation.getTaxAmount()).isEqualTo(3_000L);
        assertThat(allocation.getSellerNetAmount()).isEqualTo(90_000L);
        assertThat(allocation.getFeeConfigId()).isEqualTo(FEE_CONFIG_ID);
        assertThat(allocation.getTaxConfigId()).isEqualTo(TAX_CONFIG_ID);
    }

    private void assertWalletCredited() {
        WalletJpaEntity wallet = walletJpaRepository.findById(WALLET_ID).orElseThrow();

        assertThat(wallet.getAvailableBalance()).isEqualTo(31_000L);
        assertThat(wallet.getPendingBalance()).isEqualTo(90_000L);
    }

    private void assertCodLedgerPostingCreated(
            UUID paymentId,
            UUID orderId,
            UUID postingId
    ) {
        LedgerPostingJpaEntity posting =
                ledgerPostingJpaRepository.findById(postingId).orElseThrow();

        assertThat(posting.getPostingType()).isEqualTo("COD_CAPTURE");
        assertThat(posting.getBusinessKey())
                .isEqualTo("COD_CAPTURE:" + paymentId + ":" + orderId);
        assertThat(posting.getReferenceType()).isEqualTo("PAYMENT");
        assertThat(posting.getReferenceId()).isEqualTo(paymentId);

        List<LedgerEntryJpaEntity> entries =
                ledgerEntryJpaRepository.findByPostingId(postingId);

        assertThat(entries).hasSize(4);

        long totalDebit = entries.stream()
                .filter(entry -> "DEBIT".equals(entry.getEntryType()))
                .mapToLong(LedgerEntryJpaEntity::getAmount)
                .sum();

        long totalCredit = entries.stream()
                .filter(entry -> "CREDIT".equals(entry.getEntryType()))
                .mapToLong(LedgerEntryJpaEntity::getAmount)
                .sum();

        assertThat(totalDebit).isEqualTo(100_000L);
        assertThat(totalCredit).isEqualTo(100_000L);
    }

    private void assertPaymentSucceededOutboxCreated(UUID paymentId) {
        List<OutboxEventJpaEntity> paymentEvents = paymentOutboxEvents(paymentId);

        assertThat(paymentEvents)
                .filteredOn(event -> "payment.succeeded".equals(event.getEventType()))
                .hasSize(1);
    }

    private void assertPaymentFailedOutboxCreated(
            UUID paymentId,
            String failureCode
    ) {
        List<OutboxEventJpaEntity> paymentEvents = paymentOutboxEvents(paymentId);

        assertThat(paymentEvents)
                .filteredOn(event -> "payment.failed".equals(event.getEventType()))
                .hasSize(1);

        OutboxEventJpaEntity event = paymentEvents.stream()
                .filter(candidate -> "payment.failed".equals(candidate.getEventType()))
                .findFirst()
                .orElseThrow();

        assertThat(event.getPayload()).contains(paymentId.toString());
        assertThat(event.getPayload()).contains(failureCode);
    }

    private List<OutboxEventJpaEntity> paymentOutboxEvents(UUID paymentId) {
        return outboxEventJpaRepository
                .findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, 100))
                .stream()
                .filter(event -> paymentId.equals(event.getAggregateId()))
                .toList();
    }

    private long countOutboxEventsForPayment(UUID paymentId) {
        return paymentOutboxEvents(paymentId).size();
    }

    private static final class TestIdGenerator implements IdGeneratorPort {

        private final UUID allocationId = UUID.randomUUID();
        private final UUID postingId = UUID.randomUUID();

        @Override
        public PaymentId nextPaymentId() {
            return new PaymentId(UUID.randomUUID());
        }

        @Override
        public PaymentAllocationId nextPaymentAllocationId() {
            return new PaymentAllocationId(allocationId);
        }

        @Override
        public WalletId nextWalletId() {
            return new WalletId(UUID.randomUUID());
        }

        @Override
        public LedgerAccountId nextLedgerAccountId() {
            return new LedgerAccountId(UUID.randomUUID());
        }

        @Override
        public LedgerPostingId nextLedgerPostingId() {
            return new LedgerPostingId(postingId);
        }

        @Override
        public RefundId nextRefundId() {
            return new RefundId(UUID.randomUUID());
        }

        @Override
        public PayoutId nextPayoutId() {
            return new PayoutId(UUID.randomUUID());
        }

        @Override
        public SettlementBatchId nextSettlementBatchId() {
            return new SettlementBatchId(UUID.randomUUID());
        }

        @Override
        public SettlementBatchItemId nextSettlementBatchItemId() {
            return new SettlementBatchItemId(UUID.randomUUID());
        }

        @Override
        public SettlementLineId nextSettlementLineId() {
            return new SettlementLineId(UUID.randomUUID());
        }

        @Override
        public PaymentAttemptId nextPaymentAttemptId() {
            return new PaymentAttemptId(UUID.randomUUID());
        }
    }
}