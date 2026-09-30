package com.taca.paymentwallet.presentation.rest;

public record ApiError(
        String code,
        String message
) {

    public ApiError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "code must not be blank"
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "message must not be blank"
            );
        }

        code = code.trim();
        message = message.trim();
    }
}