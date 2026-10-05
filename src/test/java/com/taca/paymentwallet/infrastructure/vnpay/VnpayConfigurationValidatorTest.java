package com.taca.paymentwallet.infrastructure.vnpay;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VnpayConfigurationValidatorTest {

    private final VnpayConfigurationValidator validator =
            new VnpayConfigurationValidator();

    @Test
    void shouldAcceptCompleteConfiguration() {
        VnpayProperties properties =
                completeProperties();

        assertThat(
                validator.missingRequiredProperties(
                        properties
                )
        ).isEmpty();
    }

    @Test
    void shouldReportMissingRequiredProperties() {
        VnpayProperties properties =
                new VnpayProperties(
                        "",
                        "",
                        null,
                        " "
                );

        assertThat(
                validator.missingRequiredProperties(
                        properties
                )
        ).containsExactlyInAnyOrder(
                "tmnCode",
                "hashSecret",
                "paymentUrl",
                "returnUrl"
        );
    }

    @Test
    void shouldRejectIncompletePaymentUrlConfiguration() {
        VnpayProperties properties =
                new VnpayProperties(
                        "TEST_TMN",
                        "",
                        "https://sandbox.vnpayment.vn",
                        "http://localhost/payment-return"
                );

        assertThatThrownBy(
                () -> validator.validateForPaymentUrl(
                        properties
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "hashSecret"
                );
    }

    @Test
    void shouldRequireOnlyHashSecretForWebhookVerification() {
        VnpayProperties properties =
                new VnpayProperties(
                        null,
                        "TEST_SECRET",
                        null,
                        null
                );

        validator.validateForWebhook(
                properties
        );
    }

    @Test
    void shouldRejectWebhookWhenHashSecretMissing() {
        VnpayProperties properties =
                new VnpayProperties(
                        null,
                        "",
                        null,
                        null
                );

        assertThatThrownBy(
                () -> validator.validateForWebhook(
                        properties
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "VNPAY hashSecret must be configured"
                );
    }

    private VnpayProperties completeProperties() {
        return new VnpayProperties(
                "TEST_TMN",
                "TEST_SECRET",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost/payment-return"
        );
    }
}