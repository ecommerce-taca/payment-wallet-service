package com.taca.paymentwallet.application.exception;

public class ForbiddenException extends ApplicationException {

    public ForbiddenException() {
        super("Access is forbidden");
    }
}