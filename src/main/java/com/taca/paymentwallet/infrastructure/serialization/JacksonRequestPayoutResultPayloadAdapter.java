package com.taca.paymentwallet.infrastructure.serialization;

import com.taca.paymentwallet.application.port.out.RequestPayoutResultPayloadPort;
import com.taca.paymentwallet.application.result.RequestPayoutResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public class JacksonRequestPayoutResultPayloadAdapter
        implements RequestPayoutResultPayloadPort {

    private final ObjectMapper objectMapper;

    public JacksonRequestPayoutResultPayloadAdapter(
            ObjectMapper objectMapper
    ) {
        this.objectMapper =
                Objects.requireNonNull(
                        objectMapper
                );
    }

    @Override
    public String serialize(
            RequestPayoutResult result
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
                    "Unable to serialize request payout result",
                    exception
            );
        }
    }

    @Override
    public RequestPayoutResult deserialize(
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
                    RequestPayoutResult.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to deserialize request payout result",
                    exception
            );
        }
    }
}