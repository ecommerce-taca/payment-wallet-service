package com.taca.paymentwallet.infrastructure.vnpay;

import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlRequest;
import com.taca.paymentwallet.application.gateway.vnpay.CreateVnpayPaymentUrlResult;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.domain.valueobject.CheckoutGroupId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VnpayGatewayAdapterTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-01-01T00:00:00Z"
            );

    private static final Instant EXPIRES_AT =
            Instant.parse(
                    "2026-01-01T00:15:00Z"
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final UUID CHECKOUT_GROUP_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    @Test
    void shouldCreateVnpayPaymentUrl() {
        VnpayProperties properties =
                new VnpayProperties(
                        "TESTCODE",
                        "secret-key",
                        "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                        "http://localhost:3000/payment/vnpay-return"
                );

        ClockPort clockPort =
                () -> NOW;

        VnpayGatewayAdapter adapter =
                new VnpayGatewayAdapter(
                        properties,
                        new VnpaySigner(),
                        clockPort,
                        new VnpayConfigurationValidator()
                );

        CreateVnpayPaymentUrlRequest request =
                new CreateVnpayPaymentUrlRequest(
                        new PaymentId(
                                PAYMENT_ID
                        ),
                        new CheckoutGroupId(
                                CHECKOUT_GROUP_ID
                        ),
                        Money.vnd(
                                100_000
                        ),
                        EXPIRES_AT,
                        "127.0.0.1"
                );

        CreateVnpayPaymentUrlResult result =
                adapter.createPaymentUrl(
                        request
                );

        assertThat(
                result.providerTransactionRef()
        ).isEqualTo(
                "11111111111111111111111111111111"
        );

        assertThat(
                result.expiresAt()
        ).isEqualTo(
                EXPIRES_AT
        );

        assertThat(
                result.paymentUrl()
        ).startsWith(
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_Amount=10000000"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_Command=pay"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_CreateDate=20260101070000"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_CurrCode=VND"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_ExpireDate=20260101071500"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_IpAddr=127.0.0.1"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_Locale=vn"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_OrderType=other"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A3000%2Fpayment%2Fvnpay-return"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_TmnCode=TESTCODE"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_TxnRef=11111111111111111111111111111111"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_Version=2.1.0"
        );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_SecureHash="
                        + "fe9d50e178b911f8ea85e1fa74bd1b87"
                        + "ff85fe888b3fec3304317e2e5a6f6805"
                        + "a338bcaa770713f6ed22250092e16741"
                        + "a79dec5ab2822240b6984dc53f423e4c"
        );
    }

    @Test
    void shouldMultiplyAmountByOneHundred() {
        VnpayGatewayAdapter adapter =
                createAdapter();

        CreateVnpayPaymentUrlResult result =
                adapter.createPaymentUrl(
                        request(
                                12_345
                        )
                );

        assertThat(
                result.paymentUrl()
        ).contains(
                "vnp_Amount=1234500"
        );
    }

    @Test
    void shouldRejectMissingTmnCode() {
        VnpayProperties properties =
                new VnpayProperties(
                        "",
                        "secret-key",
                        "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                        "http://localhost:3000/payment/vnpay-return"
                );

        VnpayGatewayAdapter adapter =
                new VnpayGatewayAdapter(
                        properties,
                        new VnpaySigner(),
                        () -> NOW,
                        new VnpayConfigurationValidator()
                );


        assertThatThrownBy(
                () ->
                        adapter.createPaymentUrl(
                                request(
                                        100_000
                                )
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "VNPAY tmnCode must be configured"
                );
    }

    private VnpayGatewayAdapter createAdapter() {
        return new VnpayGatewayAdapter(
                new VnpayProperties(
                        "TESTCODE",
                        "secret-key",
                        "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                        "http://localhost:3000/payment/vnpay-return"
                ),
                new VnpaySigner(),
                () -> NOW,
                new VnpayConfigurationValidator()
        );
    }

    private CreateVnpayPaymentUrlRequest request(
            long amount
    ) {
        return new CreateVnpayPaymentUrlRequest(
                new PaymentId(
                        PAYMENT_ID
                ),
                new CheckoutGroupId(
                        CHECKOUT_GROUP_ID
                ),
                Money.vnd(
                        amount
                ),
                EXPIRES_AT,
                "127.0.0.1"
        );
    }
}