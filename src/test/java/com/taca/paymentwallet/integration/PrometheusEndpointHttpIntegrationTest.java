package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.port.out.BusinessMetricsPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PrometheusEndpointHttpIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("prometheus_contract_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(
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
                "app.outbox.publisher.enabled",
                () -> "false"
        );

        registry.add(
                "app.kafka.health.enabled",
                () -> "false"
        );

        registry.add(
                "app.kafka.topic-admin.enabled",
                () -> "false"
        );

        registry.add(
                "app.kafka.listeners.enabled",
                () -> "false"
        );

        registry.add(
                "vnpay.tmn-code",
                () -> "TEST_TMN"
        );

        registry.add(
                "vnpay.hash-secret",
                () -> "TEST_SECRET"
        );

        registry.add(
                "vnpay.payment-url",
                () ->
                        "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        );

        registry.add(
                "vnpay.return-url",
                () -> "http://localhost/payment-return"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessMetricsPort businessMetrics;

    @Test
    void shouldExposePrometheusScrapeEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/prometheus")
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldExposePaymentWalletMetrics()
            throws Exception {

        businessMetrics.recordPayment(
                "VNPAY",
                "SUCCESS",
                "APPLIED"
        );

        mockMvc.perform(
                        get("/prometheus")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "payment_wallet_payment_outcome_total"
                                )
                        )
                );
    }

    @Test
    void shouldNotExposeMetricsEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/metrics")
                )
                .andExpect(
                        status().isNotFound()
                );
    }

    @Test
    void shouldNotExposeSensitiveActuatorEndpoints()
            throws Exception {

        mockMvc.perform(
                        get("/env")
                )
                .andExpect(
                        status().isNotFound()
                );

        mockMvc.perform(
                        get("/beans")
                )
                .andExpect(
                        status().isNotFound()
                );
    }
}
