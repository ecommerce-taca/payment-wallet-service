package com.taca.paymentwallet.infrastructure.serialization;

import com.taca.paymentwallet.application.port.out.RequestRefundResultPayloadPort;
import com.taca.paymentwallet.application.result.RequestRefundResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public class JacksonRequestRefundResultPayloadAdapter
        implements RequestRefundResultPayloadPort {

    private final ObjectMapper objectMapper;

    public JacksonRequestRefundResultPayloadAdapter(
            ObjectMapper objectMapper
    ) {
        this.objectMapper =
                Objects.requireNonNull(
                        objectMapper
                );
    }

    @Override
    public String serialize(
            RequestRefundResult result
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
                    "Unable to serialize request refund result",
                    exception
            );
        }
    }

    @Override
    public RequestRefundResult deserialize(
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
                    RequestRefundResult.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to deserialize request refund result",
                    exception
            );
        }
    }
}