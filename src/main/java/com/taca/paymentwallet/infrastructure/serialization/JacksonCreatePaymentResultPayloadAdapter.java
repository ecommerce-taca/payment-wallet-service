package com.taca.paymentwallet.infrastructure.serialization;

import com.taca.paymentwallet.application.port.out.CreatePaymentResultPayloadPort;
import com.taca.paymentwallet.application.result.CreatePaymentResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public class JacksonCreatePaymentResultPayloadAdapter
        implements CreatePaymentResultPayloadPort {

    private final ObjectMapper objectMapper;

    public JacksonCreatePaymentResultPayloadAdapter(
            ObjectMapper objectMapper
    ) {
        this.objectMapper =
                Objects.requireNonNull(
                        objectMapper
                );
    }

    @Override
    public String serialize(
            CreatePaymentResult result
    ) {
        Objects.requireNonNull(
                result,
                "result must not be null"
        );

        try {
            return objectMapper
                    .writeValueAsString(result);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to serialize create payment result",
                    exception
            );
        }
    }

    @Override
    public CreatePaymentResult deserialize(
            String payload
    ) {
        if (payload == null
                || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "payload must not be blank"
            );
        }

        try {
            return objectMapper.readValue(
                    payload,
                    CreatePaymentResult.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to deserialize create payment result",
                    exception
            );
        }
    }
}