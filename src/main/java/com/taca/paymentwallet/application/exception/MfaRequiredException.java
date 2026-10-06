package com.taca.paymentwallet.application.exception;

public class MfaRequiredException extends ApplicationException {

    public MfaRequiredException() {
        super("MFA step-up is required");
    }
}