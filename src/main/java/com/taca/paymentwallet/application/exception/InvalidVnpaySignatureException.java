package com.taca.paymentwallet.application.exception;

public class InvalidVnpaySignatureException extends ApplicationException {

    public InvalidVnpaySignatureException() {
        super("Invalid VNPAY signature");
    }
}