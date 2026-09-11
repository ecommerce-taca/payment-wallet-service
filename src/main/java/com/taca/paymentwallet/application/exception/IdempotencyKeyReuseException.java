package com.taca.paymentwallet.application.exception;

public class IdempotencyKeyReuseException extends ApplicationException {

    public IdempotencyKeyReuseException(String idempotencyKey) {
        super("Idempotency key was reused with a different request: " + idempotencyKey);
    }
}
