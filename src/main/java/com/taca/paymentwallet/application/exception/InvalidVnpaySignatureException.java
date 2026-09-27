package com.taca.paymentwallet.application.exception;

public class InvalidVnpaySignatureException extends RuntimeException {

    public InvalidVnpaySignatureException() {
        super("Invalid VNPAY signature");
    }
}