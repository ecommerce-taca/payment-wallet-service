package com.taca.paymentwallet.infrastructure.vnpay;

import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.exception.InvalidVnpaySignatureException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VnpayWebhookVerifierAdapterTest {

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final String VALID_SIGNATURE =
            "b26cc9ce44379f8e5558e2148679d4e5"
                    + "38783805fdf3b782da4db45b6490e77e"
                    + "1f8c1230dc026bb0e55cb8c0589b4bdf"
                    + "2ee51b42e4fe7014acc39b91f071a1a6";

    @Test
    void shouldAcceptValidSignature() {
        VnpayWebhookVerifierAdapter verifier =
                createVerifier();

        Map<String, String> payload =
                new LinkedHashMap<>(
                        validPayloadWithoutSignature()
                );

        payload.put(
                "vnp_SecureHash",
                validSignature(
                        payload
                )
        );

        ProcessVnpayWebhookCommand command =
                new ProcessVnpayWebhookCommand(
                        "vnpay-event-001",
                        "vnpay-txn-001",
                        "00",
                        "00",
                        100_000,
                        "VND",
                        "payload-hash",
                        payload
                );

        assertThatCode(
                () -> verifier.verify(
                        command
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidSignature() {
        VnpayWebhookVerifierAdapter verifier =
                createVerifier();

        ProcessVnpayWebhookCommand command =
                commandWithSignature(
                        "invalid-signature"
                );

        assertThatThrownBy(
                () -> verifier.verify(
                        command
                )
        )
                .isInstanceOf(
                        InvalidVnpaySignatureException.class
                )
                .hasMessage(
                        "Invalid VNPAY signature"
                );
    }

    @Test
    void shouldRejectMissingSignature() {
        VnpayWebhookVerifierAdapter verifier =
                createVerifier();

        Map<String, String> payload =
                validPayloadWithoutSignature();

        ProcessVnpayWebhookCommand command =
                new ProcessVnpayWebhookCommand(
                        "vnpay-event-001",
                        "vnpay-txn-001",
                        "00",
                        "00",
                        100_000,
                        "VND",
                        "payload-hash",
                        Map.of(
                                "vnp_SecureHash",
                                "signed-value"
                        )
                );

        assertThatThrownBy(
                () -> verifier.verify(
                        command
                )
        )
                .isInstanceOf(
                        InvalidVnpaySignatureException.class
                );
    }

    @Test
    void shouldIgnoreSecureHashTypeWhenVerifying() {
        VnpayWebhookVerifierAdapter verifier =
                createVerifier();

        Map<String, String> payload =
                new LinkedHashMap<>(
                        validPayloadWithoutSignature()
                );

        String signature =
                validSignature(
                        payload
                );

        payload.put(
                "vnp_SecureHashType",
                "HmacSHA512"
        );

        payload.put(
                "vnp_SecureHash",
                signature
        );

        ProcessVnpayWebhookCommand command =
                new ProcessVnpayWebhookCommand(
                        "vnpay-event-001",
                        "vnpay-txn-001",
                        "00",
                        "00",
                        100_000,
                        "VND",
                        "payload-hash",
                        payload
                );

        assertThatCode(
                () -> verifier.verify(
                        command
                )
        ).doesNotThrowAnyException();
    }

    private VnpayWebhookVerifierAdapter createVerifier() {
        VnpayProperties properties =
                new VnpayProperties(
                        "TESTCODE",
                        "secret-key",
                        "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                        "http://localhost:3000/payment/vnpay-return"
                );

        return new VnpayWebhookVerifierAdapter(
                properties,
                new VnpaySigner(),
                new VnpayConfigurationValidator()
        );
    }

    private ProcessVnpayWebhookCommand commandWithSignature(
            String signature
    ) {
        Map<String, String> payload =
                new LinkedHashMap<>(
                        validPayloadWithoutSignature()
                );

        payload.put(
                "vnp_SecureHash",
                signature
        );

        return new ProcessVnpayWebhookCommand(
                "vnpay-event-001",
                "vnpay-txn-001",
                "00",
                "00",
                100_000,
                "VND",
                "payload-hash",
                Map.of(
                        "vnp_SecureHash",
                        "signed-value"
                )
        );
    }

    private Map<String, String>
    validPayloadWithoutSignature() {

        Map<String, String> payload =
                new LinkedHashMap<>();

        payload.put(
                "vnp_Amount",
                "10000000"
        );

        payload.put(
                "vnp_BankCode",
                "NCB"
        );

        payload.put(
                "vnp_BankTranNo",
                "NCB202601010001"
        );

        payload.put(
                "vnp_CardType",
                "ATM"
        );

        payload.put(
                "vnp_OrderInfo",
                "Thanh toan don hang:123"
        );

        payload.put(
                "vnp_PayDate",
                "20260101071000"
        );

        payload.put(
                "vnp_ResponseCode",
                "00"
        );

        payload.put(
                "vnp_TmnCode",
                "TESTCODE"
        );

        payload.put(
                "vnp_TransactionNo",
                "12345678"
        );

        payload.put(
                "vnp_TransactionStatus",
                "00"
        );

        payload.put(
                "vnp_TxnRef",
                "vnpay-txn-001"
        );

        return payload;
    }

    private String validSignature(
            Map<String, String> payload
    ) {
        return new VnpaySigner()
                .sign(
                        payload,
                        "secret-key"
                );
    }
}