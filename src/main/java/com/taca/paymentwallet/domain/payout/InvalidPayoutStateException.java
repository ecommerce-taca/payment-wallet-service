package com.taca.paymentwallet.domain.payout;

import com.taca.paymentwallet.domain.exception.DomainException;

public class InvalidPayoutStateException extends DomainException {

    public InvalidPayoutStateException(PayoutStatus currentStatus, String action) {
        super("Cannot " + action + " payout with status " + currentStatus);
    }
}
