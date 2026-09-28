package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.RequestPayoutCommand;
import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlResult;
import com.taca.paymentwallet.application.port.in.CreatePaymentUseCase;
import com.taca.paymentwallet.application.port.in.ProcessVnpayWebhookUseCase;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.CreatePaymentResult;
import com.taca.paymentwallet.application.service.CreatePaymentService;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.infrastructure.crypto.Sha256PaymentUrlHashAdapter;
import com.taca.paymentwallet.infrastructure.persistence.adapter.*;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;
import com.taca.paymentwallet.infrastructure.transaction.SpringTransactionAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(CreatePaymentHttpE2ETest.TestConfig.class)
class CreatePaymentHttpE2ETest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-28T10:00:00Z"
            );

    private static final UUID CHECKOUT_GROUP_ID =
            UUID.fromString(
                    "31000000-0000-0000-0000-000000000001"
            );

    private static final UUID BUYER_USER_ID =
            UUID.fromString(
                    "31000000-0000-0000-0000-000000000002"
            );

    private static final UUID ORDER_ID =
            UUID.fromString(
                    "31000000-0000-0000-0000-000000000003"
            );

    private static final UUID SHOP_ID =
            UUID.fromString(
                    "31000000-0000-0000-0000-000000000004"
            );

    private static final String PROVIDER_TRANSACTION_REF =
            "vnpay-http-e2e-txn-001";

    private static final String PAYMENT_URL =
            "https://sandbox.vnpay.vn/http-e2e-payment-url";

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.0")
                    .withDatabaseName(
                            "payment_wallet_http_e2e"
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

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentAttemptJpaRepository
            paymentAttemptJpaRepository;

    @Test
    void shouldCreateVnpayPaymentThroughHttpAndPersistIt()
            throws Exception {

        String body =
                """
                {
                  "checkout_group_id":
                    "31000000-0000-0000-0000-000000000001",
                  "buyer_user_id":
                    "31000000-0000-0000-0000-000000000002",
                  "method": "VNPAY",
                  "amount": 100000,
                  "currency": "VND",
                  "orders": [
                    {
                      "order_id":
                        "31000000-0000-0000-0000-000000000003",
                      "shop_id":
                        "31000000-0000-0000-0000-000000000004",
                      "amount": 100000
                    }
                  ]
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/payments"
                        )
                                .header(
                                        "Idempotency-Key",
                                        "http-e2e-idem-001"
                                )
                                .header(
                                        "X-Request-ID",
                                        "http-e2e-request-001"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        body
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath(
                                "$.data.checkout_group_id"
                        ).value(
                                CHECKOUT_GROUP_ID.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.status"
                        ).value(
                                "PENDING"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.method"
                        ).value(
                                "VNPAY"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.amount"
                        ).value(
                                100_000
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.currency"
                        ).value(
                                "VND"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.data.payment_url"
                        ).value(
                                PAYMENT_URL
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.meta.request_id"
                        ).value(
                                "http-e2e-request-001"
                        )
                );

        var payment =
                paymentJpaRepository
                        .findByCheckoutGroupId(
                                CHECKOUT_GROUP_ID
                        )
                        .orElseThrow();

        assertThat(
                payment.getCheckoutGroupId()
        ).isEqualTo(
                CHECKOUT_GROUP_ID
        );

        assertThat(
                payment.getBuyerUserId()
        ).isEqualTo(
                BUYER_USER_ID
        );

        assertThat(
                payment.getMethod()
        ).isEqualTo(
                "VNPAY"
        );

        assertThat(
                payment.getStatus()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                payment.getAmount()
        ).isEqualTo(
                100_000L
        );

        assertThat(
                payment.getCurrency()
        ).isEqualTo(
                "VND"
        );

        var attempt =
                paymentAttemptJpaRepository
                        .findByProviderAndProviderTransactionRef(
                                "VNPAY",
                                PROVIDER_TRANSACTION_REF
                        )
                        .orElseThrow();

        assertThat(
                attempt.getPaymentId()
        ).isEqualTo(
                payment.getId()
        );

        assertThat(
                attempt.getProvider()
        ).isEqualTo(
                "VNPAY"
        );

        assertThat(
                attempt.getStatus()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                attempt.getProviderTransactionRef()
        ).isEqualTo(
                PROVIDER_TRANSACTION_REF
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ClockPort clockPort() {
            return () -> NOW;
        }

        @Bean
        TransactionPort transactionPort(
                PlatformTransactionManager transactionManager
        ) {
            return new SpringTransactionAdapter(
                    transactionManager
            );
        }

        @Bean
        PaymentRepositoryPort paymentRepositoryPort(
                PaymentJpaRepository paymentRepository,
                PaymentOrderJpaRepository orderRepository,
                ClockPort clockPort
        ) {
            return new PaymentRepositoryAdapter(
                    paymentRepository,
                    orderRepository,
                    new PaymentPersistenceMapper(),
                    clockPort,
                    new PersistenceUuidGenerator()
            );
        }

        @Bean
        PaymentAttemptRepositoryPort paymentAttemptRepositoryPort(
                PaymentAttemptJpaRepository repository,
                ClockPort clockPort
        ) {
            return new PaymentAttemptRepositoryAdapter(
                    repository,
                    new PaymentAttemptPersistenceMapper(),
                    clockPort
            );
        }

        @Bean
        IdempotencyPort idempotencyPort(
                IdempotencyKeyJpaRepository repository,
                ClockPort clockPort
        ) {
            return new IdempotencyPersistenceAdapter(
                    repository,
                    clockPort,
                    new PersistenceUuidGenerator(),
                    Duration.ofHours(
                            24
                    )
            );
        }

        @Bean
        PaymentUrlHashPort paymentUrlHashPort() {
            return new Sha256PaymentUrlHashAdapter();
        }

        @Bean
        RequestHashPort requestHashPort() {
            return new TestRequestHashPort();
        }

        @Bean
        IdGeneratorPort idGeneratorPort() {
            return new TestIdGeneratorPort();
        }

        @Bean
        VnpayGatewayPort vnpayGatewayPort() {
            return request ->
                    new CreateVnpayPaymentUrlResult(
                            PROVIDER_TRANSACTION_REF,
                            PAYMENT_URL,
                            request.expiresAt()
                    );
        }

        @Bean
        OutboxPort outboxPort(
                OutboxEventJpaRepository repository,
                ObjectMapper objectMapper
        ) {
            return new OutboxPersistenceAdapter(
                    repository,
                    objectMapper
            );
        }

        @Bean
        CreatePaymentResultPayloadPort
        createPaymentResultPayloadPort(
                ObjectMapper objectMapper
        ) {
            return new TestCreatePaymentResultPayloadPort(
                    objectMapper
            );
        }

        @Bean
        CreatePaymentUseCase createPaymentUseCase(
                PaymentRepositoryPort paymentRepository,
                PaymentAttemptRepositoryPort paymentAttemptRepository,
                IdempotencyPort idempotencyPort,
                RequestHashPort requestHashPort,
                PaymentUrlHashPort paymentUrlHashPort,
                IdGeneratorPort idGeneratorPort,
                ClockPort clockPort,
                VnpayGatewayPort vnpayGatewayPort,
                OutboxPort outboxPort,
                TransactionPort transactionPort,
                CreatePaymentResultPayloadPort resultPayloadPort
        ) {
            return new CreatePaymentService(
                    paymentRepository,
                    paymentAttemptRepository,
                    idempotencyPort,
                    requestHashPort,
                    paymentUrlHashPort,
                    idGeneratorPort,
                    clockPort,
                    vnpayGatewayPort,
                    outboxPort,
                    transactionPort,
                    resultPayloadPort
            );
        }

        /*
         * PaymentController cần bean này để context được tạo.
         * 3C.1 chưa test webhook.
         */
        @Bean
        ProcessVnpayWebhookUseCase
        processVnpayWebhookUseCase() {
            return command -> {
                throw new UnsupportedOperationException(
                        "Webhook is not part of CreatePaymentHttpE2ETest"
                );
            };
        }
    }

    private static final class TestRequestHashPort
            implements RequestHashPort {

        @Override
        public String hash(
                CreatePaymentCommand command
        ) {
            return "a".repeat(
                    64
            );
        }

        @Override
        public String hash(
                RequestRefundCommand command
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String hash(
                RequestPayoutCommand command
        ) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestCreatePaymentResultPayloadPort
            implements CreatePaymentResultPayloadPort {

        private final ObjectMapper objectMapper;

        private TestCreatePaymentResultPayloadPort(
                ObjectMapper objectMapper
        ) {
            this.objectMapper = objectMapper;
        }

        @Override
        public String serialize(
                CreatePaymentResult result
        ) {
            try {
                return objectMapper.writeValueAsString(
                        result
                );
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Unable to serialize CreatePaymentResult",
                        exception
                );
            }
        }

        @Override
        public CreatePaymentResult deserialize(
                String payload
        ) {
            try {
                return objectMapper.readValue(
                        payload,
                        CreatePaymentResult.class
                );
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Unable to deserialize CreatePaymentResult",
                        exception
                );
            }
        }
    }

    private static final class TestIdGeneratorPort
            implements IdGeneratorPort {

        @Override
        public PaymentId nextPaymentId() {
            return new PaymentId(
                    UUID.randomUUID()
            );
        }

        @Override
        public PaymentAttemptId nextPaymentAttemptId() {
            return new PaymentAttemptId(
                    UUID.randomUUID()
            );
        }

        @Override
        public PaymentAllocationId
        nextPaymentAllocationId() {
            return new PaymentAllocationId(
                    UUID.randomUUID()
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
                    UUID.randomUUID()
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