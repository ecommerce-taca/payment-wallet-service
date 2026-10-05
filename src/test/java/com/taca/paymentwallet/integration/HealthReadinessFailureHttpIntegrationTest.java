package com.taca.paymentwallet.integration;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class HealthReadinessFailureHttpIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("health_failure_test")
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
                "vnpay.tmn-code",
                () -> ""
        );

        registry.add(
                "vnpay.hash-secret",
                () -> ""
        );

        registry.add(
                "vnpay.payment-url",
                () -> "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        );

        registry.add(
                "vnpay.return-url",
                () -> "http://localhost/payment-return"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldKeepLivenessUpWhenVnpayConfigurationIsMissing()
            throws Exception {

        mockMvc.perform(
                        get("/health/live")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("UP")
                );
    }

    @Test
    void shouldMarkReadinessUnavailableWhenVnpayConfigurationIsMissing()
            throws Exception {

        mockMvc.perform(
                        get("/health/ready")
                )
                .andExpect(
                        status().isServiceUnavailable()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("OUT_OF_SERVICE")
                );
    }
}