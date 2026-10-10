package com.taca.paymentwallet.application.query;

import java.util.UUID;

public record GetPaymentQuery(UUID paymentId) {

    public GetPaymentQuery {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }
    }
}