package com.taca.paymentwallet.presentation.rest;

import java.util.Objects;

public record ApiResponse<T>(
        T data,
        ApiMeta meta
) {

    public ApiResponse {
        Objects.requireNonNull(
                data,
                "data must not be null"
        );

        Objects.requireNonNull(
                meta,
                "meta must not be null"
        );
    }
}