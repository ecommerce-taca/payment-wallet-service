package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.OrderId;

public class PaymentNotFoundByOrderException extends RuntimeException {

    public PaymentNotFoundByOrderException(OrderId orderId) {
        super("Payment not found for order " + orderId.value());
    }
}