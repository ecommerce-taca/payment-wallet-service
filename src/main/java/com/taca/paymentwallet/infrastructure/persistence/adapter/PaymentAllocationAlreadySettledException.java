package com.taca.paymentwallet.infrastructure.persistence.adapter;

import java.util.UUID;

public class PaymentAllocationAlreadySettledException extends RuntimeException {

    public PaymentAllocationAlreadySettledException(
            UUID paymentAllocationId
    ) {
        super(
                "payment allocation already settled: "
                        + paymentAllocationId
        );
    }
}