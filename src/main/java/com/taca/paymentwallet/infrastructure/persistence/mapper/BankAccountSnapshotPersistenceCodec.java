package com.taca.paymentwallet.infrastructure.persistence.mapper;

import com.taca.paymentwallet.domain.payout.BankAccountSnapshot;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BankAccountSnapshotPersistenceCodec {

    private static final String VERSION = "v1";

    public String encode(
            BankAccountSnapshot snapshot
    ) {
        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "snapshot must not be null"
            );
        }

        return String.join(
                ".",
                VERSION,
                encodePart(snapshot.bankCode()),
                encodePart(snapshot.accountHolderName()),
                encodePart(snapshot.maskedAccountNumber())
        );
    }

    public BankAccountSnapshot decode(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "bank account snapshot must not be blank"
            );
        }

        String[] parts = value.split("\\.", -1);

        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "invalid bank account snapshot format"
            );
        }

        if (!VERSION.equals(parts[0])) {
            throw new IllegalArgumentException(
                    "unsupported bank account snapshot version"
            );
        }

        return new BankAccountSnapshot(
                decodePart(parts[1]),
                decodePart(parts[2]),
                decodePart(parts[3])
        );
    }

    private String encodePart(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }

    private String decodePart(String value) {
        return new String(
                Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }
}