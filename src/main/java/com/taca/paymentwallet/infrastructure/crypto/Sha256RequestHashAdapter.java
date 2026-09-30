package com.taca.paymentwallet.infrastructure.crypto;

import com.taca.paymentwallet.application.command.CreatePaymentCommand;
import com.taca.paymentwallet.application.command.RequestPayoutCommand;
import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.port.out.RequestHashPort;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

public class Sha256RequestHashAdapter
        implements RequestHashPort {

    private final ObjectMapper objectMapper;

    public Sha256RequestHashAdapter(
            ObjectMapper objectMapper
    ) {
        this.objectMapper =
                Objects.requireNonNull(objectMapper);
    }

    @Override
    public String hash(
            CreatePaymentCommand command
    ) {
        return hashValue(command);
    }

    @Override
    public String hash(
            RequestRefundCommand command
    ) {
        return hashValue(command);
    }

    @Override
    public String hash(
            RequestPayoutCommand command
    ) {
        return hashValue(command);
    }

    private String hashValue(
            Object value
    ) {
        Objects.requireNonNull(
                value,
                "value must not be null"
        );

        try {
            String json =
                    objectMapper.writeValueAsString(
                            value
                    );

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            json.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to calculate request hash",
                    exception
            );
        }
    }
}