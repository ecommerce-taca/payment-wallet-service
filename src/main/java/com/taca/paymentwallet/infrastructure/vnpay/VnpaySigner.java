package com.taca.paymentwallet.infrastructure.vnpay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class VnpaySigner {

    private static final String HMAC_SHA_512 =
            "HmacSHA512";

    public String sign(
            Map<String, String> parameters,
            String hashSecret
    ) {
        if (parameters == null) {
            throw new IllegalArgumentException(
                    "parameters must not be null"
            );
        }

        if (hashSecret == null
                || hashSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "hashSecret must not be blank"
            );
        }

        String hashData =
                buildHashData(parameters);

        try {
            Mac mac =
                    Mac.getInstance(
                            HMAC_SHA_512
                    );

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            hashSecret.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            HMAC_SHA_512
                    );

            mac.init(secretKey);

            byte[] result =
                    mac.doFinal(
                            hashData.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return toHex(result);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to calculate VNPAY signature",
                    exception
            );
        }
    }

    public String buildHashData(
            Map<String, String> parameters
    ) {
        Objects.requireNonNull(
                parameters,
                "parameters must not be null"
        );

        return parameters.entrySet()
                .stream()
                .filter(entry ->
                        entry.getKey() != null
                                && !entry.getKey().isBlank()
                )
                .filter(entry ->
                        entry.getValue() != null
                                && !entry.getValue().isBlank()
                )
                .sorted(
                        Map.Entry.comparingByKey(
                                Comparator.naturalOrder()
                        )
                )
                .map(entry ->
                        entry.getKey()
                                + "="
                                + entry.getValue()
                )
                .collect(
                        Collectors.joining("&")
                );
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