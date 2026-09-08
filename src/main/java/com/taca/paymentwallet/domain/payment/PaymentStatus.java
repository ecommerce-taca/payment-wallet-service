package com.taca.paymentwallet.domain.payment;

public enum PaymentStatus {
    PENDING,
    PENDING_COD,
    SUCCESS,
    FAILED,
    EXPIRED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
