package com.taca.paymentwallet.infrastructure.vnpay;

import com.taca.paymentwallet.application.command.ProcessVnpayWebhookCommand;
import com.taca.paymentwallet.application.exception.InvalidVnpaySignatureException;
import com.taca.paymentwallet.application.port.out.VnpayWebhookVerifierPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class VnpayWebhookVerifierAdapter
        implements VnpayWebhookVerifierPort {

    private static final String SECURE_HASH =
            "vnp_SecureHash";

    private static final String SECURE_HASH_TYPE =
            "vnp_SecureHashType";

    private final VnpayProperties properties;

    private final VnpaySigner signer;

    public VnpayWebhookVerifierAdapter(
            VnpayProperties properties,
            VnpaySigner signer
    ) {
        this.properties = Objects.requireNonNull(properties);

        this.signer = Objects.requireNonNull(signer);
    }

    @Override
    public void verify(
            ProcessVnpayWebhookCommand command
    ) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        validateConfiguration();

        Map<String, String> payload =
                command.signedPayload();

        String providedSignature =
                payload.get(SECURE_HASH);

        if (providedSignature == null || providedSignature.isBlank()) {
            throw new InvalidVnpaySignatureException();
        }

        Map<String, String> signedFields =
                new LinkedHashMap<>(payload);

        signedFields.remove(
                SECURE_HASH
        );

        signedFields.remove(
                SECURE_HASH_TYPE
        );

        String expectedSignature =
                signer.sign(
                        signedFields,
                        properties.hashSecret()
                );

        if (!constantTimeEquals(
                expectedSignature,
                providedSignature
        )) {
            throw new InvalidVnpaySignatureException();
        }
    }

    private boolean constantTimeEquals(
            String expected,
            String actual
    ) {
        byte[] expectedBytes =
                expected
                        .toLowerCase()
                        .getBytes(
                                StandardCharsets.US_ASCII
                        );

        byte[] actualBytes =
                actual
                        .trim()
                        .toLowerCase()
                        .getBytes(
                                StandardCharsets.US_ASCII
                        );

        return MessageDigest.isEqual(
                expectedBytes,
                actualBytes
        );
    }

    private void validateConfiguration() {
        if (properties.hashSecret() == null
                || properties.hashSecret().isBlank()) {
            throw new IllegalStateException(
                    "VNPAY hashSecret must be configured"
            );
        }
    }
}