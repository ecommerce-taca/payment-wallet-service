package com.taca.paymentwallet.domain.payment;

import com.taca.paymentwallet.domain.exception.DomainException;

public class InvalidPaymentStateException extends DomainException {

    public InvalidPaymentStateException(PaymentStatus currentStatus, String action) {
        super("Cannot " + action + " payment with status " + currentStatus);
    }
}