package com.taca.paymentwallet.domain.settlement;

import com.taca.paymentwallet.domain.exception.DomainException;

public class InvalidSettlementStateException extends DomainException {

    public InvalidSettlementStateException(String message) {
        super(message);
    }
}
