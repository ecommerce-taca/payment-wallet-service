package com.taca.paymentwallet.infrastructure.crypto;

import com.taca.paymentwallet.application.port.out.PaymentUrlHashPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256PaymentUrlHashAdapter
        implements PaymentUrlHashPort {

    @Override
    public String hash(
            String paymentUrl
    ) {
        if (paymentUrl == null
                || paymentUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "paymentUrl must not be blank"
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            paymentUrl.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return toHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private String toHex(
            byte[] bytes
    ) {
        StringBuilder result =
                new StringBuilder(
                        bytes.length * 2
                );

        for (byte value : bytes) {
            result.append(
                    String.format(
                            "%02x",
                            value & 0xff
                    )
            );
        }

        return result.toString();
    }
}