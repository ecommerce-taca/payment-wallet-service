package com.taca.paymentwallet.infrastructure.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256PaymentUrlHashAdapterTest {

    private final Sha256PaymentUrlHashAdapter adapter =
            new Sha256PaymentUrlHashAdapter();

    @Test
    void shouldHashPaymentUrlUsingSha256() {
        String hash =
                adapter.hash(
                        "https://sandbox.vnpayment.vn/payment-url"
                );

        assertThat(
                hash
        ).hasSize(
                64
        );

        assertThat(
                hash
        ).matches(
                "[0-9a-f]{64}"
        );
    }

    @Test
    void shouldProduceDeterministicHash() {
        String url =
                "https://sandbox.vnpayment.vn/payment-url";

        assertThat(
                adapter.hash(url)
        ).isEqualTo(
                adapter.hash(url)
        );
    }

    @Test
    void shouldRejectBlankPaymentUrl() {
        assertThatThrownBy(
                () ->
                        adapter.hash(
                                " "
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "paymentUrl must not be blank"
                );
    }
}