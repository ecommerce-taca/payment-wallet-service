package com.taca.paymentwallet.application.exception;

import com.taca.paymentwallet.domain.valueobject.PayoutId;

public class PayoutNotFoundException extends ApplicationException {

    public PayoutNotFoundException(PayoutId payoutId) {
        super("Payout not found: " + payoutId.value());
    }
}