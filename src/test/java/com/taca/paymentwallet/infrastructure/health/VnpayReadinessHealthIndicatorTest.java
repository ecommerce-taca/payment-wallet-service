package com.taca.paymentwallet.infrastructure.health;

import com.taca.paymentwallet.infrastructure.vnpay.VnpayConfigurationValidator;
import com.taca.paymentwallet.infrastructure.vnpay.VnpayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VnpayReadinessHealthIndicatorTest {

    private final VnpayConfigurationValidator validator =
            new VnpayConfigurationValidator();

    @Test
    void shouldBeUpWhenVnpayConfigurationIsComplete() {
        VnpayReadinessHealthIndicator indicator =
                new VnpayReadinessHealthIndicator(
                        new VnpayProperties(
                                "TEST_TMN",
                                "TEST_SECRET",
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "http://localhost/payment-return"
                        ),
                        validator
                );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(Status.UP);

        assertThat(health.getDetails())
                .containsEntry(
                        "provider",
                        "VNPAY"
                )
                .containsEntry(
                        "configured",
                        true
                );
    }

    @Test
    void shouldBeOutOfServiceWhenRequiredConfigurationIsMissing() {
        VnpayReadinessHealthIndicator indicator =
                new VnpayReadinessHealthIndicator(
                        new VnpayProperties(
                                "",
                                "",
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "http://localhost/payment-return"
                        ),
                        validator
                );

        var health = indicator.health();

        assertThat(health.getStatus())
                .isEqualTo(
                        Status.OUT_OF_SERVICE
                );

        assertThat(health.getDetails())
                .containsEntry(
                        "provider",
                        "VNPAY"
                )
                .containsEntry(
                        "configured",
                        false
                );

        assertThat(
                health.getDetails()
                        .get("missingProperties")
        ).isEqualTo(
                Set.of(
                        "tmnCode",
                        "hashSecret"
                )
        );
    }

    @Test
    void shouldNeverExposeSecretValue() {
        String secret =
                "VERY_SECRET_VALUE";

        VnpayReadinessHealthIndicator indicator =
                new VnpayReadinessHealthIndicator(
                        new VnpayProperties(
                                "TEST_TMN",
                                secret,
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                "http://localhost/payment-return"
                        ),
                        validator
                );

        String health =
                indicator.health()
                        .getDetails()
                        .toString();

        assertThat(health)
                .doesNotContain(secret);
    }
}