package com.taca.paymentwallet.application.exception;

public class RequestAlreadyProcessingException extends ApplicationException {

    public RequestAlreadyProcessingException(String idempotencyKey) {
        super("Request is already processing for idempotency key: " + idempotencyKey);
    }
}
