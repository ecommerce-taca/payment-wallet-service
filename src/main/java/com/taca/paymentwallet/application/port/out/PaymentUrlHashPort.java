package com.taca.paymentwallet.application.port.out;

public interface PaymentUrlHashPort {

    String hash(String paymentUrl);
}