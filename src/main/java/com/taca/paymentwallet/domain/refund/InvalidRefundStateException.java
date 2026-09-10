package com.taca.paymentwallet.domain.refund;

import com.taca.paymentwallet.domain.exception.DomainException;

public class InvalidRefundStateException extends DomainException {

    public InvalidRefundStateException(RefundStatus currentStatus, String action) {
        super("Cannot " + action + " refund with status " + currentStatus);
    }
}