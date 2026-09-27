package com.taca.paymentwallet.infrastructure.vnpay;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VnpaySignerTest {

    private final VnpaySigner signer =
            new VnpaySigner();

    @Test
    void shouldBuildHashDataInSortedOrder() {
        Map<String, String> parameters =
                new LinkedHashMap<>();

        parameters.put(
                "vnp_TmnCode",
                "TESTCODE"
        );

        parameters.put(
                "vnp_Amount",
                "10000000"
        );

        parameters.put(
                "vnp_Command",
                "pay"
        );

        String hashData =
                signer.buildHashData(
                        parameters
                );

        assertThat(
                hashData
        ).isEqualTo(
                "vnp_Amount=10000000"
                        + "&vnp_Command=pay"
                        + "&vnp_TmnCode=TESTCODE"
        );
    }

    @Test
    void shouldIgnoreNullAndBlankValues() {
        Map<String, String> parameters =
                new LinkedHashMap<>();

        parameters.put(
                "vnp_Amount",
                "10000000"
        );

        parameters.put(
                "vnp_BankCode",
                ""
        );

        parameters.put(
                "vnp_Locale",
                null
        );

        parameters.put(
                "vnp_TmnCode",
                "TESTCODE"
        );

        String hashData =
                signer.buildHashData(
                        parameters
                );

        assertThat(
                hashData
        ).isEqualTo(
                "vnp_Amount=10000000"
                        + "&vnp_TmnCode=TESTCODE"
        );
    }

    @Test
    void shouldCreateHmacSha512Signature() {
        Map<String, String> parameters =
                Map.of(
                        "vnp_TmnCode",
                        "TESTCODE",
                        "vnp_Amount",
                        "10000000",
                        "vnp_Command",
                        "pay"
                );

        String signature =
                signer.sign(
                        parameters,
                        "secret"
                );

        assertThat(
                signature
        ).isEqualTo(
                "3c4796b35debf5717375801566c2fb1c"
                        + "94b9f8eed264be929de17a7ed3c65cb9"
                        + "6a09388b14f235e19f320bc334c1aaf2"
                        + "2cabea7b1411b654251c98b828a4c021"
        );
    }

    @Test
    void shouldRejectBlankSecret() {
        assertThatThrownBy(
                () ->
                        signer.sign(
                                Map.of(
                                        "vnp_Amount",
                                        "10000000"
                                ),
                                " "
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "hashSecret must not be blank"
                );
    }

    @Test
    void shouldUrlEncodeValuesWhenBuildingHashData() {
        Map<String, String> parameters =
                Map.of(
                        "vnp_OrderInfo",
                        "Thanh toan don hang:123",
                        "vnp_ReturnUrl",
                        "http://localhost:3000/payment/vnpay-return",
                        "vnp_TmnCode",
                        "TESTCODE"
                );

        String hashData =
                signer.buildHashData(
                        parameters
                );

        assertThat(
                hashData
        ).isEqualTo(
                "vnp_OrderInfo=Thanh+toan+don+hang%3A123"
                        + "&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A3000%2Fpayment%2Fvnpay-return"
                        + "&vnp_TmnCode=TESTCODE"
        );

        assertThat(
                signer.sign(
                        parameters,
                        "secret-key"
                )
        ).isEqualTo(
                "3b357c27bb4f240fdc1b99b6be2a8da1"
                        + "9d7764cf38dc11596a2c0328decaf385"
                        + "25eb0bb43efa5fdbcc4ae70e6abbed93"
                        + "8aa71c5ac3e9e8389d6437876b4cb219"
        );
    }
}