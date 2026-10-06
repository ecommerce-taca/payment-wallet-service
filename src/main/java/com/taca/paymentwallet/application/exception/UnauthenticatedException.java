package com.taca.paymentwallet.application.exception;

public class UnauthenticatedException extends ApplicationException {

    public UnauthenticatedException() {
        super("Authentication is required");
    }
}