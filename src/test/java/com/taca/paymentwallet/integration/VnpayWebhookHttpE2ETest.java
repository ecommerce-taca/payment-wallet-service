package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.domain.payment.Payment;
import com.taca.paymentwallet.domain.payment.PaymentAttempt;
import com.taca.paymentwallet.domain.payment.PaymentMethod;
import com.taca.paymentwallet.domain.payment.PaymentOrder;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.infrastructure.persistence.adapter.*;
import com.taca.paymentwallet.infrastructure.persistence.entity.*;
import com.taca.paymentwallet.infrastructure.persistence.mapper.*;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import com.taca.paymentwallet.infrastructure.transaction.SpringTransactionAdapter;
import com.taca.paymentwallet.infrastructure.vnpay.VnpaySigner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(VnpayWebhookHttpE2ETest.TestConfig.class)
class VnpayWebhookHttpE2ETest {

    private static final UUID SHOP_ID =
            UUID.fromString(
                    "20000000-0000-0000-0000-000000000001"
            );

    private static final UUID WALLET_ID =
            UUID.fromString(
                    "21000000-0000-0000-0000-000000000001"
            );

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-25T09:30:00Z"
            );

    private static final Instant EXPIRES_AT =
            Instant.parse(
                    "2026-09-25T10:00:00Z"
            );

    private static final String PAYLOAD_HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private static final String VNPAY_HASH_SECRET =
            "http-e2e-secret-key";

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName(
                            "payment_wallet_webhook_http_e2e"
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
                "app.outbox.publisher.enabled",
                () -> false
        );

        registry.add(
                "vnpay.tmn-code",
                () -> "TESTCODE"
        );

        registry.add(
                "vnpay.hash-secret",
                () -> VNPAY_HASH_SECRET
        );

        registry.add(
                "vnpay.payment-url",
                () ->
                        "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        );

        registry.add(
                "vnpay.return-url",
                () ->
                        "http://localhost/vnpay-return"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentOrderJpaRepository paymentOrderJpaRepository;

    @Autowired
    private PaymentAttemptJpaRepository paymentAttemptJpaRepository;

    @Autowired
    private PaymentAllocationJpaRepository
            paymentAllocationJpaRepository;

    @Autowired
    private WalletJpaRepository walletJpaRepository;

    @Autowired
    private LedgerPostingJpaRepository ledgerPostingJpaRepository;

    @Autowired
    private LedgerEntryJpaRepository ledgerEntryJpaRepository;

    @Autowired
    private PaymentEventJpaRepository paymentEventJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldProcessSuccessfulWebhookAndIgnoreDuplicate()
            throws Exception {

        UUID paymentId =
                UUID.randomUUID();

        UUID checkoutGroupId =
                UUID.randomUUID();

        UUID orderId =
                UUID.randomUUID();

        String providerTransactionRef =
                "vnpay-http-e2e-"
                        + paymentId
                        .toString()
                        .replace("-", "");

        String providerEventId =
                "vnpay-http-e2e-event-"
                        + paymentId
                        .toString()
                        .replace("-", "");

        seedPendingPayment(
                paymentId,
                checkoutGroupId,
                orderId,
                providerTransactionRef
        );

        WalletJpaEntity walletBefore =
                walletJpaRepository
                        .findById(
                                WALLET_ID
                        )
                        .orElseThrow();

        long pendingBefore =
                walletBefore.getPendingBalance();

        String body = validWebhookBody(
                providerTransactionRef,
                providerEventId
        );

        /*
         * ===== First webhook =====
         */
        mockMvc.perform(
                        post("/api/v1/payments/webhook")
                                .header(
                                        "X-Request-ID",
                                        "req-webhook-http-e2e-001"
                                )
                                .header(
                                        "traceparent",
                                        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
                                )
                                .header(
                                        "tracestate",
                                        "vendor=value"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(body)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_id"
                        ).value(
                                paymentId.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_status"
                        ).value(
                                "SUCCESS"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.action"
                        ).value(
                                "APPLIED"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "req-webhook-http-e2e-001"
                        )
                );

        assertPaymentSucceeded(
                paymentId
        );

        assertAttemptSucceeded(providerTransactionRef);

        assertAllocationCreated(
                paymentId,
                orderId
        );

        assertWalletCredited(
                pendingBefore
        );

        assertProviderEventApplied(
                paymentId,
                providerEventId
        );

        assertOutboxCreated(
                paymentId
        );

        long allocationCountAfterFirst =
                paymentAllocationJpaRepository.count();

        long ledgerPostingCountAfterFirst =
                ledgerPostingJpaRepository.count();

        long ledgerEntryCountAfterFirst =
                ledgerEntryJpaRepository.count();

        long outboxCountAfterFirst =
                outboxEventJpaRepository.count();

        long paymentEventCountAfterFirst =
                paymentEventJpaRepository.count();

        long pendingAfterFirst =
                walletJpaRepository
                        .findById(
                                WALLET_ID
                        )
                        .orElseThrow()
                        .getPendingBalance();

        /*
         * ===== Same webhook again =====
         */
        mockMvc.perform(
                        post(
                                "/api/v1/payments/webhook"
                        )
                                .header(
                                        "X-Request-ID",
                                        "req-webhook-http-e2e-002"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        body
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_id"
                        ).value(
                                paymentId.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_status"
                        ).value(
                                "SUCCESS"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.action"
                        ).value(
                                "DUPLICATE"
                        )
                );

        /*
         * Duplicate webhook MUST NOT create
         * financial effects again.
         */
        assertThat(
                paymentAllocationJpaRepository.count()
        ).isEqualTo(
                allocationCountAfterFirst
        );

        assertThat(
                ledgerPostingJpaRepository.count()
        ).isEqualTo(
                ledgerPostingCountAfterFirst
        );

        assertThat(
                ledgerEntryJpaRepository.count()
        ).isEqualTo(
                ledgerEntryCountAfterFirst
        );

        assertThat(
                outboxEventJpaRepository.count()
        ).isEqualTo(
                outboxCountAfterFirst
        );

        assertThat(
                paymentEventJpaRepository.count()
        ).isEqualTo(
                paymentEventCountAfterFirst
        );

        assertThat(
                walletJpaRepository
                        .findById(
                                WALLET_ID
                        )
                        .orElseThrow()
                        .getPendingBalance()
        ).isEqualTo(
                pendingAfterFirst
        );
    }

    private String validWebhookBody(
            String providerTransactionRef,
            String providerEventId
    )
            throws Exception {

        Map<String, String> signedPayload =
                new LinkedHashMap<>();

        signedPayload.put(
                "vnp_Amount",
                "10000000"
        );

        signedPayload.put(
                "vnp_ResponseCode",
                "00"
        );

        signedPayload.put(
                "vnp_TransactionStatus",
                "00"
        );

        signedPayload.put(
                "vnp_TxnRef",
                providerTransactionRef
        );

        String signature =
                new VnpaySigner()
                        .sign(
                                signedPayload,
                                VNPAY_HASH_SECRET
                        );

        signedPayload.put(
                "vnp_SecureHash",
                signature
        );

        Map<String, Object> request =
                new LinkedHashMap<>();

        request.put(
                "provider_event_id",
                providerEventId
        );

        request.put(
                "provider_transaction_ref",
                providerTransactionRef
        );

        request.put(
                "response_code",
                "00"
        );

        request.put(
                "transaction_status",
                "00"
        );

        request.put(
                "amount",
                100_000
        );

        request.put(
                "currency",
                "VND"
        );

        request.put(
                "payload_hash",
                PAYLOAD_HASH
        );

        request.put(
                "signed_payload",
                signedPayload
        );

        return objectMapper
                .writeValueAsString(
                        request
                );
    }

    private void seedPendingPayment(
            UUID paymentId,
            UUID checkoutGroupId,
            UUID orderId,
            String providerTransactionRef
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

        PaymentAttempt attempt =
                PaymentAttempt.create(
                        new PaymentAttemptId(
                                UUID.randomUUID()
                        ),
                        new PaymentId(
                                paymentId
                        ),
                        "VNPAY",
                        providerTransactionRef,
                        "a".repeat(64),
                        "b".repeat(64),
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

        PaymentAttemptRepositoryAdapter attemptRepository =
                new PaymentAttemptRepositoryAdapter(
                        paymentAttemptJpaRepository,
                        new PaymentAttemptPersistenceMapper(),
                        clockPort
                );

        TransactionPort transactionPort =
                new SpringTransactionAdapter(
                        transactionManager
                );

        transactionPort.execute(
                () -> {
                    paymentRepository.save(
                            payment
                    );

                    attemptRepository.save(
                            attempt
                    );
                }
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

    private void assertAttemptSucceeded(String providerTransactionRef) {
        PaymentAttemptJpaEntity attempt =
                paymentAttemptJpaRepository
                        .findByProviderAndProviderTransactionRef(
                                "VNPAY",
                                providerTransactionRef
                        )
                        .orElseThrow();

        assertThat(
                attempt.getStatus()
        ).isEqualTo(
                "SUCCESS"
        );

        assertThat(
                attempt.getCompletedAt()
        ).isEqualTo(
                PersistenceTimeMapper
                        .toLocalDateTime(
                                NOW
                        )
        );

        assertThat(
                attempt.getFailureCode()
        ).isNull();
    }

    private void assertAllocationCreated(
            UUID paymentId,
            UUID orderId
    ) {
        List<PaymentAllocationJpaEntity> allocations =
                paymentAllocationJpaRepository
                        .findByPaymentIdOrderByCreatedAtAscIdAsc(
                                paymentId
                        );

        assertThat(
                allocations
        ).hasSize(
                1
        );

        PaymentAllocationJpaEntity allocation =
                allocations.getFirst();

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
    }

    private void assertWalletCredited(
            long pendingBefore
    ) {
        WalletJpaEntity wallet =
                walletJpaRepository
                        .findById(
                                WALLET_ID
                        )
                        .orElseThrow();

        assertThat(
                wallet.getPendingBalance()
        ).isEqualTo(
                pendingBefore
                        + 90_000L
        );
    }

    private void assertProviderEventApplied(
            UUID paymentId,
            String providerEventId
    ) {
        PaymentEventJpaEntity event =
                paymentEventJpaRepository
                        .findByProviderAndProviderEventId(
                                "VNPAY",
                                providerEventId

                        )
                        .orElseThrow();

        assertThat(
                event.getPaymentId()
        ).isEqualTo(
                paymentId
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
    }

    private void assertOutboxCreated(
            UUID paymentId
    ) {
        List<OutboxEventJpaEntity> events =
                outboxEventJpaRepository
                        .findAll()
                        .stream()
                        .filter(
                                event ->
                                        paymentId.equals(
                                                event.getAggregateId()
                                        )
                        )
                        .toList();

        assertThat(
                events
        ).hasSize(
                1
        );

        assertThat(
                events.getFirst()
                        .getEventType()
        ).isEqualTo(
                "payment.succeeded"
        );

        OutboxEventJpaEntity outboxEvent =
                events.getFirst();

        assertThat(
                outboxEvent.getHeaders()
        ).isNotNull();

        assertThat(
                outboxEvent.getHeaders()
        ).contains(
                outboxEvent.getId()
                        .toString()
        );

        assertThat(
                outboxEvent.getHeaders()
        ).contains(
                "req-webhook-http-e2e-001"
        );

        assertThat(
                outboxEvent.getHeaders()
        ).contains(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );

        assertThat(
                outboxEvent.getHeaders()
        ).contains(
                "vendor=value"
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        ClockPort testClockPort() {
            return () -> NOW;
        }
    }
}