package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.RefundId;

public class RefundNotFoundException extends ApplicationException {

    public RefundNotFoundException(RefundId refundId) {
        super("Refund not found: " + refundId.value());
    }
}