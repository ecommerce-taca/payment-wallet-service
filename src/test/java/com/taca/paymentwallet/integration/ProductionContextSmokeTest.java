package com.taca.paymentwallet.integration;

import com.taca.paymentwallet.application.port.in.CreatePaymentUseCase;
import com.taca.paymentwallet.application.port.in.ProcessVnpayWebhookUseCase;
import com.taca.paymentwallet.application.port.in.RequestPayoutUseCase;
import com.taca.paymentwallet.application.port.in.RequestRefundUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(
        disabledWithoutDocker = true
)
class ProductionContextSmokeTest {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(
                    "mysql:8.4"
            )
                    .withDatabaseName(
                            "paymentdb"
                    )
                    .withUsername(
                            "payment_user"
                    )
                    .withPassword(
                            "payment_password"
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
                () ->
                        "http://localhost/payment-return"
        );
    }

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Autowired
    private ProcessVnpayWebhookUseCase processVnpayWebhookUseCase;

    @Autowired
    private RequestRefundUseCase requestRefundUseCase;

    @Autowired
    private RequestPayoutUseCase requestPayoutUseCase;

    @Test
    void shouldStartProductionContextWithoutTestConfiguration() {
        assertThat(createPaymentUseCase).isNotNull();

        assertThat(processVnpayWebhookUseCase).isNotNull();

        assertThat(requestRefundUseCase).isNotNull();

        assertThat(requestPayoutUseCase).isNotNull();
    }
}