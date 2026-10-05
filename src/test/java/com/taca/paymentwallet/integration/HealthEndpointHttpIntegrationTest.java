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
class HealthEndpointHttpIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("health_contract_test")
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
                () -> "TEST_TMN"
        );

        registry.add(
                "vnpay.hash-secret",
                () -> "TEST_SECRET"
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
    void shouldExposeLiveEndpoint() throws Exception {
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
    void shouldExposeReadyEndpointWhenDependenciesAreHealthy()
            throws Exception {

        mockMvc.perform(
                        get("/health/ready")
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
    void shouldKeepConventionalProbeEndpoints()
            throws Exception {

        mockMvc.perform(
                        get("/health/liveness")
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        get("/health/readiness")
                )
                .andExpect(
                        status().isOk()
                );
    }
}