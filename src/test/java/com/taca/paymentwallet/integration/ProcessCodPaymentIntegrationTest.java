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
                    .withDatabaseName(
                            "payment_wallet_cod_e2e_test"
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
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldProcessDeliveredCodPaymentEndToEnd() {
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

        WalletRepositoryAdapter walletRepository =
                new WalletRepositoryAdapter(
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

        TestIdGenerator idGenerator =
                new TestIdGenerator();

        ProcessCodPaymentService service =
                new ProcessCodPaymentService(
                        paymentRepository,
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

        UUID paymentId =
                UUID.randomUUID();

        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID orderId =
                UUID.randomUUID();

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
                        PaymentMethod.COD,
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
                        )
                );

        /*
         * Setup payment cũng chạy bằng transaction
         * infrastructure thật.
         */
        transactionPort.execute(
                () ->
                        paymentRepository.save(
                                payment
                        )
        );

        ProcessCodPaymentResult result =
                service.execute(
                        new ProcessCodPaymentCommand(
                                checkoutGroupId,
                                CodPaymentResultStatus.DELIVERED,
                                100_000,
                                "VND",
                                DELIVERED_AT,
                                null
                        )
                );

        assertThat(
                result.action()
        ).isEqualTo(
                CodPaymentProcessingAction.APPLIED
        );

        assertThat(
                result.paymentStatus()
        ).isEqualTo(
                "SUCCESS"
        );

        assertPaymentSucceeded(
                paymentId
        );

        assertAllocationCreated(
                paymentId,
                orderId,
                idGenerator.allocationId
        );

        assertWalletCredited();

        assertLedgerPostingCreated(
                paymentId,
                idGenerator.postingId
        );

        assertOutboxEventCreated(
                paymentId
        );
    }

    @Test
    void shouldProcessFailedCodPaymentEndToEnd() {
        UUID paymentId =
                UUID.randomUUID();

        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID orderId =
                UUID.randomUUID();

        createCodPayment(
                paymentId,
                checkoutGroupId,
                orderId
        );

        WalletJpaEntity walletBefore =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        long availableBalanceBefore =
                walletBefore.getAvailableBalance();

        long pendingBalanceBefore =
                walletBefore.getPendingBalance();

        ProcessCodPaymentService service =
                createCodPaymentService();

        ProcessCodPaymentResult result =
                service.execute(
                        new ProcessCodPaymentCommand(
                                checkoutGroupId,
                                CodPaymentResultStatus.FAILED,
                                100_000,
                                "VND",
                                DELIVERED_AT,
                                "SHIPMENT_FAILED"
                        )
                );

        assertThat(
                result.action()
        ).isEqualTo(
                CodPaymentProcessingAction.APPLIED
        );

        assertThat(
                result.paymentStatus()
        ).isEqualTo(
                "FAILED"
        );

        PaymentJpaEntity storedPayment =
                paymentJpaRepository
                        .findById(paymentId)
                        .orElseThrow();

        assertThat(
                storedPayment.getStatus()
        ).isEqualTo(
                "FAILED"
        );

        assertThat(
                storedPayment.getFailureCode()
        ).isEqualTo(
                "SHIPMENT_FAILED"
        );

        assertThat(
                storedPayment.getCapturedAmount()
        ).isZero();

        assertThat(
                storedPayment.getPaidAt()
        ).isNull();

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId
                        )
        ).isEmpty();

        assertThat(
                ledgerPostingJpaRepository
                        .findByBusinessKey(
                                "PAYMENT_CAPTURE:"
                                        + paymentId
                        )
        ).isEmpty();

        WalletJpaEntity walletAfter =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        assertThat(
                walletAfter.getAvailableBalance()
        ).isEqualTo(
                availableBalanceBefore
        );

        assertThat(
                walletAfter.getPendingBalance()
        ).isEqualTo(
                pendingBalanceBefore
        );

        assertPaymentFailedOutboxEventCreated(
                paymentId,
                "SHIPMENT_FAILED"
        );
    }

    @Test
    void shouldProcessCancelledCodPaymentEndToEnd() {
        UUID paymentId =
                UUID.randomUUID();

        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID orderId =
                UUID.randomUUID();

        createCodPayment(
                paymentId,
                checkoutGroupId,
                orderId
        );

        WalletJpaEntity walletBefore =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        long availableBalanceBefore =
                walletBefore.getAvailableBalance();

        long pendingBalanceBefore =
                walletBefore.getPendingBalance();

        ProcessCodPaymentService service =
                createCodPaymentService();

        ProcessCodPaymentResult result =
                service.execute(
                        new ProcessCodPaymentCommand(
                                checkoutGroupId,
                                CodPaymentResultStatus.CANCELLED,
                                100_000,
                                "VND",
                                DELIVERED_AT,
                                "ORDER_CANCELLED"
                        )
                );

        assertThat(
                result.action()
        ).isEqualTo(
                CodPaymentProcessingAction.APPLIED
        );

        assertThat(
                result.paymentStatus()
        ).isEqualTo(
                "FAILED"
        );

        PaymentJpaEntity storedPayment =
                paymentJpaRepository
                        .findById(paymentId)
                        .orElseThrow();

        assertThat(
                storedPayment.getStatus()
        ).isEqualTo(
                "FAILED"
        );

        assertThat(
                storedPayment.getFailureCode()
        ).isEqualTo(
                "ORDER_CANCELLED"
        );

        assertThat(
                storedPayment.getCapturedAmount()
        ).isZero();

        assertThat(
                storedPayment.getPaidAt()
        ).isNull();

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId
                        )
        ).isEmpty();

        assertThat(
                ledgerPostingJpaRepository
                        .findByBusinessKey(
                                "PAYMENT_CAPTURE:"
                                        + paymentId
                        )
        ).isEmpty();

        WalletJpaEntity walletAfter =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        assertThat(
                walletAfter.getAvailableBalance()
        ).isEqualTo(
                availableBalanceBefore
        );

        assertThat(
                walletAfter.getPendingBalance()
        ).isEqualTo(
                pendingBalanceBefore
        );

        assertPaymentFailedOutboxEventCreated(
                paymentId,
                "ORDER_CANCELLED"
        );
    }

    @Test
    void shouldIgnoreDuplicateDeliveredCodPaymentEndToEnd() {
        UUID paymentId =
                UUID.randomUUID();

        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID orderId =
                UUID.randomUUID();

        createCodPayment(
                paymentId,
                checkoutGroupId,
                orderId
        );

        ProcessCodPaymentService service =
                createCodPaymentService();

        ProcessCodPaymentResult firstResult =
                service.execute(
                        new ProcessCodPaymentCommand(
                                checkoutGroupId,
                                CodPaymentResultStatus.DELIVERED,
                                100_000,
                                "VND",
                                DELIVERED_AT,
                                null
                        )
                );

        assertThat(
                firstResult.action()
        ).isEqualTo(
                CodPaymentProcessingAction.APPLIED
        );

        assertThat(
                firstResult.paymentStatus()
        ).isEqualTo(
                "SUCCESS"
        );

        WalletJpaEntity walletAfterFirstCall =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        long pendingBalanceAfterFirstCall =
                walletAfterFirstCall
                        .getPendingBalance();

        List<PaymentAllocationJpaEntity>
                allocationsAfterFirstCall =
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId
                        );

        assertThat(
                allocationsAfterFirstCall
        ).hasSize(1);

        long outboxCountAfterFirstCall =
                countOutboxEventsForPayment(
                        paymentId
                );

        assertThat(
                outboxCountAfterFirstCall
        ).isEqualTo(1L);

        ProcessCodPaymentResult duplicateResult =
                service.execute(
                        new ProcessCodPaymentCommand(
                                checkoutGroupId,
                                CodPaymentResultStatus.DELIVERED,
                                100_000,
                                "VND",
                                DELIVERED_AT,
                                null
                        )
                );

        assertThat(
                duplicateResult.action()
        ).isEqualTo(
                CodPaymentProcessingAction.DUPLICATE
        );

        assertThat(
                duplicateResult.paymentStatus()
        ).isEqualTo(
                "SUCCESS"
        );

        PaymentJpaEntity storedPayment =
                paymentJpaRepository
                        .findById(paymentId)
                        .orElseThrow();

        assertThat(
                storedPayment.getStatus()
        ).isEqualTo(
                "SUCCESS"
        );

        assertThat(
                storedPayment.getCapturedAmount()
        ).isEqualTo(
                100_000L
        );

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId
                        )
        ).hasSize(1);

        WalletJpaEntity walletAfterDuplicate =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        assertThat(
                walletAfterDuplicate
                        .getPendingBalance()
        ).isEqualTo(
                pendingBalanceAfterFirstCall
        );

        assertThat(
                ledgerPostingJpaRepository
                        .findByBusinessKey(
                                "PAYMENT_CAPTURE:"
                                        + paymentId
                        )
        ).isPresent();

        assertThat(
                countOutboxEventsForPayment(
                        paymentId
                )
        ).isEqualTo(
                outboxCountAfterFirstCall
        );
    }

    @Test
    void shouldIgnoreDuplicateFailedCodPaymentEndToEnd() {
        UUID paymentId =
                UUID.randomUUID();

        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID orderId =
                UUID.randomUUID();

        createCodPayment(
                paymentId,
                checkoutGroupId,
                orderId
        );

        ProcessCodPaymentService service =
                createCodPaymentService();

        ProcessCodPaymentResult firstResult =
                service.execute(
                        new ProcessCodPaymentCommand(
                                checkoutGroupId,
                                CodPaymentResultStatus.FAILED,
                                100_000,
                                "VND",
                                DELIVERED_AT,
                                "SHIPMENT_FAILED"
                        )
                );

        assertThat(
                firstResult.action()
        ).isEqualTo(
                CodPaymentProcessingAction.APPLIED
        );

        assertThat(
                firstResult.paymentStatus()
        ).isEqualTo(
                "FAILED"
        );

        WalletJpaEntity walletAfterFirstCall =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        long availableBalanceAfterFirstCall =
                walletAfterFirstCall
                        .getAvailableBalance();

        long pendingBalanceAfterFirstCall =
                walletAfterFirstCall
                        .getPendingBalance();

        long outboxCountAfterFirstCall =
                countOutboxEventsForPayment(
                        paymentId
                );

        assertThat(
                outboxCountAfterFirstCall
        ).isEqualTo(1L);

        ProcessCodPaymentResult duplicateResult =
                service.execute(
                        new ProcessCodPaymentCommand(
                                checkoutGroupId,
                                CodPaymentResultStatus.FAILED,
                                100_000,
                                "VND",
                                DELIVERED_AT,
                                "SHIPMENT_FAILED"
                        )
                );

        assertThat(
                duplicateResult.action()
        ).isEqualTo(
                CodPaymentProcessingAction.DUPLICATE
        );

        assertThat(
                duplicateResult.paymentStatus()
        ).isEqualTo(
                "FAILED"
        );

        PaymentJpaEntity storedPayment =
                paymentJpaRepository
                        .findById(paymentId)
                        .orElseThrow();

        assertThat(
                storedPayment.getStatus()
        ).isEqualTo(
                "FAILED"
        );

        assertThat(
                storedPayment.getFailureCode()
        ).isEqualTo(
                "SHIPMENT_FAILED"
        );

        assertThat(
                storedPayment.getCapturedAmount()
        ).isZero();

        assertThat(
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId
                        )
        ).isEmpty();

        assertThat(
                ledgerPostingJpaRepository
                        .findByBusinessKey(
                                "PAYMENT_CAPTURE:"
                                        + paymentId
                        )
        ).isEmpty();

        WalletJpaEntity walletAfterDuplicate =
                walletJpaRepository
                        .findById(WALLET_ID)
                        .orElseThrow();

        assertThat(
                walletAfterDuplicate
                        .getAvailableBalance()
        ).isEqualTo(
                availableBalanceAfterFirstCall
        );

        assertThat(
                walletAfterDuplicate
                        .getPendingBalance()
        ).isEqualTo(
                pendingBalanceAfterFirstCall
        );

        assertThat(
                countOutboxEventsForPayment(
                        paymentId
                )
        ).isEqualTo(
                outboxCountAfterFirstCall
        );
    }

    private long countOutboxEventsForPayment(
            UUID paymentId
    ) {
        return outboxEventJpaRepository
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
                .count();
    }

    private ProcessCodPaymentService createCodPaymentService() {
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

        WalletRepositoryAdapter walletRepository =
                new WalletRepositoryAdapter(
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
                paymentRepository,
                allocationRepository,
                walletRepository,
                ledgerRepository,
                accountLookup,
                feePolicy,
                new TestIdGenerator(),
                outbox,
                transactionPort,
                new AllocationCalculator(),
                new LedgerPostingFactory()
        );
    }

    private void createCodPayment(
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
                        PaymentMethod.COD,
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
                        )
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
                () -> paymentRepository.save(
                        payment
                )
        );
    }

    private void assertPaymentFailedOutboxEventCreated(
            UUID paymentId,
            String failureCode
    ) {
        List<OutboxEventJpaEntity> unpublished =
                outboxEventJpaRepository
                        .findByPublishedAtIsNullOrderByOccurredAtAsc(
                                PageRequest.of(
                                        0,
                                        100
                                )
                        );

        List<OutboxEventJpaEntity> paymentEvents =
                unpublished.stream()
                        .filter(event ->
                                paymentId.equals(
                                        event.getAggregateId()
                                )
                        )
                        .toList();

        assertThat(
                paymentEvents
        ).hasSize(1);

        OutboxEventJpaEntity event =
                paymentEvents.getFirst();

        assertThat(
                event.getAggregateType()
        ).isEqualTo(
                "PAYMENT"
        );

        assertThat(
                event.getEventType()
        ).isEqualTo(
                "payment.failed"
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

        assertThat(
                event.getPayload()
        ).contains(
                failureCode
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
                                DELIVERED_AT
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

    private void assertWalletCredited() {
        WalletJpaEntity wallet =
                walletJpaRepository
                        .findById(
                                WALLET_ID
                        )
                        .orElseThrow();

        /*
         * Seed V014:
         * available = 31,000
         * pending   = 0
         */
        assertThat(
                wallet.getAvailableBalance()
        ).isEqualTo(
                31_000L
        );

        assertThat(
                wallet.getPendingBalance()
        ).isEqualTo(
                90_000L
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

        long totalDebit =
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

        long totalCredit =
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
                totalDebit
        ).isEqualTo(
                100_000L
        );

        assertThat(
                totalCredit
        ).isEqualTo(
                100_000L
        );
    }

    private void assertOutboxEventCreated(
            UUID paymentId
    ) {
        List<OutboxEventJpaEntity> unpublished =
                outboxEventJpaRepository
                        .findByPublishedAtIsNullOrderByOccurredAtAsc(
                                PageRequest.of(
                                        0,
                                        100
                                )
                        );

        List<OutboxEventJpaEntity> paymentEvents =
                unpublished.stream()
                        .filter(event ->
                                paymentId.equals(
                                        event.getAggregateId()
                                )
                        )
                        .toList();

        assertThat(
                paymentEvents
        ).hasSize(1);

        OutboxEventJpaEntity event =
                paymentEvents.getFirst();

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

    private static class TestIdGenerator
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
        public PaymentAllocationId
        nextPaymentAllocationId() {
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
        public LedgerAccountId
        nextLedgerAccountId() {
            return new LedgerAccountId(
                    UUID.randomUUID()
            );
        }

        @Override
        public LedgerPostingId
        nextLedgerPostingId() {
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
        public SettlementBatchId
        nextSettlementBatchId() {
            return new SettlementBatchId(
                    UUID.randomUUID()
            );
        }

        @Override
        public SettlementBatchItemId
        nextSettlementBatchItemId() {
            return new SettlementBatchItemId(
                    UUID.randomUUID()
            );
        }

        @Override
        public SettlementLineId
        nextSettlementLineId() {
            return new SettlementLineId(
                    UUID.randomUUID()
            );
        }
    }
}