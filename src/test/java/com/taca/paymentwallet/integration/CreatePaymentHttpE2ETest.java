package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlResult;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.domain.valueobject.*;
import com.taca.paymentwallet.infrastructure.persistence.adapter.*;
import com.taca.paymentwallet.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
        @Primary
        VnpayGatewayPort testVnpayGatewayPort() {
            return request ->
                    new CreateVnpayPaymentUrlResult(
                            PROVIDER_TRANSACTION_REF,
                            PAYMENT_URL,
                            request.expiresAt()
                    );
        }
    }
}