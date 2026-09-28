package com.taca.paymentwallet.presentation.rest;

import java.util.Objects;

public record ApiErrorResponse(
        ApiError error,
        ApiMeta meta
) {

    public ApiErrorResponse {
        Objects.requireNonNull(
                error,
                "error must not be null"
        );

        Objects.requireNonNull(
                meta,
                "meta must not be null"
        );
    }
}