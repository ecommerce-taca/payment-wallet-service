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
}