package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.InboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.LedgerPostingJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAllocationJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentAttemptJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentEventJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.PaymentJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.entity.WalletJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PersistenceRepositoryIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("payment_wallet_repository_test")
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
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private PaymentAttemptJpaRepository paymentAttemptRepository;

    @Autowired
    private PaymentEventJpaRepository paymentEventRepository;

    @Autowired
    private WalletJpaRepository walletRepository;

    @Autowired
    private FeeConfigJpaRepository feeConfigRepository;

    @Autowired
    private TaxConfigJpaRepository taxConfigRepository;

    @Autowired
    private PaymentAllocationJpaRepository paymentAllocationRepository;

    @Autowired
    private LedgerAccountJpaRepository ledgerAccountRepository;

    @Autowired
    private LedgerPostingJpaRepository ledgerPostingRepository;

    @Autowired
    private LedgerEntryJpaRepository ledgerEntryRepository;

    @Autowired
    private IdempotencyKeyJpaRepository idempotencyKeyRepository;

    @Autowired
    private InboxEventJpaRepository inboxEventRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventRepository;

    @Autowired
    private AuditLogJpaRepository auditLogRepository;

    @Test
    void shouldPersistAndFindPaymentByCheckoutGroupId() {
        PaymentJpaEntity payment = newPayment("PENDING");
        paymentRepository.saveAndFlush(payment);

        assertThat(paymentRepository.findByCheckoutGroupId(payment.getCheckoutGroupId()))
                .isPresent()
                .get()
                .extracting(PaymentJpaEntity::getId)
                .isEqualTo(payment.getId());

        assertThat(paymentRepository.findByIdForUpdate(payment.getId()))
                .isPresent();
    }

    @Test
    void shouldPersistPaymentAttemptAndPaymentEvent() {
        PaymentJpaEntity payment = paymentRepository.saveAndFlush(newPayment("PENDING"));

        PaymentAttemptJpaEntity attempt = new PaymentAttemptJpaEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setPaymentId(payment.getId());
        attempt.setProvider("VNPAY");
        attempt.setProviderTransactionRef("VNPAY-" + UUID.randomUUID());
        attempt.setStatus("PENDING");
        attempt.setRequestHash("a".repeat(64));
        attempt.setPaymentUrlHash("b".repeat(64));
        attempt.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        attempt.setCreatedAt(LocalDateTime.now());
        attempt.setUpdatedAt(LocalDateTime.now());
        paymentAttemptRepository.saveAndFlush(attempt);

        PaymentEventJpaEntity event = new PaymentEventJpaEntity();
        event.setId(UUID.randomUUID());
        event.setPaymentId(payment.getId());
        event.setPaymentAttemptId(attempt.getId());
        event.setProvider("VNPAY");
        event.setProviderEventId("EVENT-" + UUID.randomUUID());
        event.setProviderTransactionRef(attempt.getProviderTransactionRef());
        event.setProviderResponseCode("00");
        event.setProviderTransactionStatus("SUCCESS");
        event.setAmount(100_000L);
        event.setCurrency("VND");
        event.setPayloadHash("c".repeat(64));
        event.setReceivedAt(LocalDateTime.now());
        event.setStatus("RECEIVED");
        paymentEventRepository.saveAndFlush(event);

        assertThat(paymentAttemptRepository.findByProviderAndProviderTransactionRef(
                "VNPAY",
                attempt.getProviderTransactionRef()
        )).isPresent();

        assertThat(paymentEventRepository.findByProviderAndProviderEventId(
                "VNPAY",
                event.getProviderEventId()
        )).isPresent();
    }

    @Test
    void shouldPersistWalletFeeTaxAndPaymentAllocation() {
        PaymentJpaEntity payment = paymentRepository.saveAndFlush(newPayment("SUCCESS"));
        WalletJpaEntity wallet = walletRepository.saveAndFlush(newWallet());
        FeeConfigJpaEntity feeConfig = feeConfigRepository.saveAndFlush(newFeeConfig());
        TaxConfigJpaEntity taxConfig = taxConfigRepository.saveAndFlush(newTaxConfig());

        PaymentAllocationJpaEntity allocation = new PaymentAllocationJpaEntity();
        allocation.setId(UUID.randomUUID());
        allocation.setPaymentId(payment.getId());
        allocation.setOrderId(UUID.randomUUID());
        allocation.setShopId(wallet.getShopId());
        allocation.setWalletId(wallet.getId());
        allocation.setGrossAmount(100_000L);
        allocation.setCommissionAmount(7_000L);
        allocation.setTaxAmount(3_000L);
        allocation.setSellerNetAmount(90_000L);
        allocation.setCurrency("VND");
        allocation.setFeeConfigId(feeConfig.getId());
        allocation.setTaxConfigId(taxConfig.getId());
        allocation.setCreatedAt(LocalDateTime.now());
        paymentAllocationRepository.saveAndFlush(allocation);

        assertThat(walletRepository.findByShopIdAndCurrency(wallet.getShopId(), "VND"))
                .isPresent();

        assertThat(feeConfigRepository
                .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        "PLATFORM",
                        LocalDateTime.now()
                ))
                .isPresent();

        assertThat(taxConfigRepository
                .findFirstByScopeAndCategoryIdIsNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        "PLATFORM",
                        LocalDateTime.now()
                ))
                .isPresent();

        assertThat(paymentAllocationRepository.findByPaymentId(payment.getId()))
                .hasSize(1);
    }

    @Test
    void shouldPersistLedgerPostingAndEntries() {
        LedgerAccountJpaEntity debitAccount = ledgerAccountRepository.saveAndFlush(
                newSystemLedgerAccount("TEST_VNPAY_CLEARING:" + UUID.randomUUID(), "VNPAY_CLEARING")
        );
        LedgerAccountJpaEntity creditAccount = ledgerAccountRepository.saveAndFlush(
                newSystemLedgerAccount("TEST_PLATFORM_COMMISSION:" + UUID.randomUUID(), "PLATFORM_COMMISSION")
        );

        LedgerPostingJpaEntity posting = new LedgerPostingJpaEntity();
        posting.setId(UUID.randomUUID());
        posting.setPostingType("PAYMENT_CAPTURE");
        posting.setBusinessKey("PAYMENT_CAPTURE:" + UUID.randomUUID());
        posting.setReferenceType("PAYMENT");
        posting.setReferenceId(UUID.randomUUID());
        posting.setDescription("Repository integration test posting");
        posting.setCreatedAt(LocalDateTime.now());
        ledgerPostingRepository.saveAndFlush(posting);

        LedgerEntryJpaEntity debit = new LedgerEntryJpaEntity();
        debit.setId(UUID.randomUUID());
        debit.setPostingId(posting.getId());
        debit.setAccountId(debitAccount.getId());
        debit.setEntryType("DEBIT");
        debit.setAmount(10_000L);
        debit.setCreatedAt(LocalDateTime.now());

        LedgerEntryJpaEntity credit = new LedgerEntryJpaEntity();
        credit.setId(UUID.randomUUID());
        credit.setPostingId(posting.getId());
        credit.setAccountId(creditAccount.getId());
        credit.setEntryType("CREDIT");
        credit.setAmount(10_000L);
        credit.setCreatedAt(LocalDateTime.now());

        ledgerEntryRepository.saveAndFlush(debit);
        ledgerEntryRepository.saveAndFlush(credit);

        assertThat(ledgerPostingRepository.existsByBusinessKey(posting.getBusinessKey()))
                .isTrue();

        assertThat(ledgerEntryRepository.findByPostingId(posting.getId()))
                .hasSize(2);
    }

    @Test
    void shouldPersistOperationalEntities() {
        IdempotencyKeyJpaEntity idempotencyKey = new IdempotencyKeyJpaEntity();
        idempotencyKey.setId(UUID.randomUUID());
        idempotencyKey.setScope("PAYMENT");
        idempotencyKey.setScopeId(UUID.randomUUID().toString());
        idempotencyKey.setIdempotencyKey("idem-" + UUID.randomUUID());
        idempotencyKey.setRequestHash("request-hash");
        idempotencyKey.setResponseSnapshot("{\"status\":\"ok\"}");
        idempotencyKey.setStatus("SUCCEEDED");
        idempotencyKey.setCreatedAt(LocalDateTime.now());
        idempotencyKey.setExpiresAt(LocalDateTime.now().plusDays(1));
        idempotencyKeyRepository.saveAndFlush(idempotencyKey);

        InboxEventJpaEntity inboxEvent = new InboxEventJpaEntity();
        inboxEvent.setId(UUID.randomUUID());
        inboxEvent.setConsumerName("payment-wallet-test-consumer");
        inboxEvent.setSource("shipment-service");
        inboxEvent.setEventId("event-" + UUID.randomUUID());
        inboxEvent.setEventType("shipment.delivered");
        inboxEvent.setPayloadHash("payload-hash");
        inboxEvent.setReceivedAt(LocalDateTime.now());
        inboxEvent.setProcessedAt(LocalDateTime.now());
        inboxEvent.setStatus("PROCESSED");
        inboxEventRepository.saveAndFlush(inboxEvent);

        OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateType("PAYMENT");
        outboxEvent.setAggregateId(UUID.randomUUID());
        outboxEvent.setEventType("payment.succeeded");
        outboxEvent.setPayload("{\"amount\":100000,\"currency\":\"VND\"}");
        outboxEvent.setHeaders("{\"traceId\":\"repository-test\"}");
        outboxEvent.setOccurredAt(LocalDateTime.now());
        outboxEvent.setRetryCount(0);
        outboxEventRepository.saveAndFlush(outboxEvent);

        AuditLogJpaEntity auditLog = new AuditLogJpaEntity();
        auditLog.setId(UUID.randomUUID());
        auditLog.setActorUserId(null);
        auditLog.setActorType("SYSTEM");
        auditLog.setAction("REPOSITORY_TEST");
        auditLog.setTargetType("PAYMENT");
        auditLog.setTargetId(UUID.randomUUID());
        auditLog.setReason("Repository integration test");
        auditLog.setMetadata("{\"source\":\"test\"}");
        auditLog.setOccurredAt(LocalDateTime.now());
        auditLogRepository.saveAndFlush(auditLog);

        assertThat(idempotencyKeyRepository.findByScopeAndScopeIdAndIdempotencyKey(
                idempotencyKey.getScope(),
                idempotencyKey.getScopeId(),
                idempotencyKey.getIdempotencyKey()
        )).isPresent();

        assertThat(inboxEventRepository.findByConsumerNameAndSourceAndEventId(
                inboxEvent.getConsumerName(),
                inboxEvent.getSource(),
                inboxEvent.getEventId()
        )).isPresent();

        assertThat(outboxEventRepository.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, 10)))
                .extracting(OutboxEventJpaEntity::getId)
                .contains(outboxEvent.getId());

        assertThat(auditLogRepository.findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
                auditLog.getTargetType(),
                auditLog.getTargetId()
        )).hasSize(1);
    }

    private PaymentJpaEntity newPayment(String status) {
        PaymentJpaEntity payment = new PaymentJpaEntity();
        payment.setId(UUID.randomUUID());
        payment.setCheckoutGroupId(UUID.randomUUID());
        payment.setBuyerUserId(UUID.randomUUID());
        payment.setMethod("VNPAY");
        payment.setAmount(100_000L);
        payment.setCurrency("VND");
        payment.setStatus(status);
        payment.setCapturedAmount("SUCCESS".equals(status) ? 100_000L : 0L);
        payment.setRefundedAmount(0L);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        payment.setPaidAt("SUCCESS".equals(status) ? LocalDateTime.now() : null);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    private WalletJpaEntity newWallet() {
        WalletJpaEntity wallet = new WalletJpaEntity();
        wallet.setId(UUID.randomUUID());
        wallet.setShopId(UUID.randomUUID());
        wallet.setCurrency("VND");
        wallet.setAvailableBalance(0L);
        wallet.setPendingBalance(0L);
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        return wallet;
    }

    private FeeConfigJpaEntity newFeeConfig() {
        FeeConfigJpaEntity config = new FeeConfigJpaEntity();
        config.setId(UUID.randomUUID());
        config.setScope("PLATFORM");
        config.setCategoryId(null);
        config.setRateBps(700);
        config.setEffectiveFrom(LocalDateTime.now().minusDays(1));
        config.setNote("Repository integration test fee");
        config.setCreatedBy(UUID.randomUUID());
        config.setCreatedAt(LocalDateTime.now());
        return config;
    }

    private TaxConfigJpaEntity newTaxConfig() {
        TaxConfigJpaEntity config = new TaxConfigJpaEntity();
        config.setId(UUID.randomUUID());
        config.setScope("PLATFORM");
        config.setCategoryId(null);
        config.setRateBps(300);
        config.setEffectiveFrom(LocalDateTime.now().minusDays(1));
        config.setNote("Repository integration test tax");
        config.setCreatedBy(UUID.randomUUID());
        config.setCreatedAt(LocalDateTime.now());
        return config;
    }

    private LedgerAccountJpaEntity newSystemLedgerAccount(String accountCode, String accountType) {
        LedgerAccountJpaEntity account = new LedgerAccountJpaEntity();
        account.setId(UUID.randomUUID());
        account.setAccountCode(accountCode);
        account.setAccountType(accountType);
        account.setOwnerType("SYSTEM");
        account.setOwnerId(null);
        account.setCurrency("VND");
        account.setStatus("ACTIVE");
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}