package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;

public class PaymentAllocationNotFoundException extends ApplicationException {

    public PaymentAllocationNotFoundException(PaymentAllocationId allocationId) {
        super("payment allocation not found: " + allocationId.value());
    }
}