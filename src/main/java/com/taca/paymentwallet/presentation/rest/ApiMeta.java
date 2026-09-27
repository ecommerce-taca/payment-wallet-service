package com.taca.paymentwallet.presentation.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiMeta(

        @JsonProperty("request_id")
        String requestId
) {

    public ApiMeta {
        if (requestId == null
                || requestId.isBlank()) {
            throw new IllegalArgumentException(
                    "requestId must not be blank"
            );
        }

        requestId =
                requestId.trim();
    }
}